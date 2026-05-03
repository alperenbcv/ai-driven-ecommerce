/**
 * Declarative Jenkins Pipeline
 *
 * GitHub Actions vs Jenkins karşılaştırması:
 * ┌──────────────────────────┬──────────────────────────────────┬────────────────────────────────────┐
 * │ Kavram                   │ GitHub Actions (.yml)            │ Jenkins (Declarative Pipeline)     │
 * ├──────────────────────────┼──────────────────────────────────┼────────────────────────────────────┤
 * │ Pipeline tanımı          │ YAML workflow dosyası             │ Groovy DSL (Jenkinsfile)            │
 * │ Tetikleyici              │ on: push / pull_request / cron   │ triggers { ... } veya Webhook       │
 * │ Paralel iş               │ strategy: matrix                 │ parallel { stage('x') { ... } }    │
 * │ Gizli değişken           │ secrets.MY_SECRET                │ credentials('MY_SECRET')            │
 * │ Artifact saklama         │ upload-artifact action           │ archiveArtifacts / stash/unstash    │
 * │ Koşul                    │ if: always() / needs             │ when { ... } / post { always { } } │
 * │ Altyapı gereklilikleri   │ GitHub tarafından sağlanır        │ Jenkins controller + agent gerekir  │
 * │ Docker agent             │ runs-on: ubuntu-latest           │ agent { docker { image '...' } }    │
 * └──────────────────────────┴──────────────────────────────────┴────────────────────────────────────┘
 */

pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21-alpine'
            args  '-v $HOME/.m2:/root/.m2'
        }
    }

    environment {
        AWS_REGION       = 'eu-central-1'
        ECR_REGISTRY     = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_TAG        = "${env.GIT_COMMIT[0..7]}"

        AWS_CREDENTIALS  = credentials('aws-credentials')
        SLACK_WEBHOOK    = credentials('slack-webhook')
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo "Branch: ${env.BRANCH_NAME} | Commit: ${env.GIT_COMMIT}"
            }
        }

        stage('Build & Test') {
            steps {
                sh './mvnw verify --batch-mode -T 1C'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: '**/target/surefire-reports/*.xml', allowEmptyArchive: true
                }
            }
        }

        stage('Jib → ECR (Paralel)') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    aws configure set aws_access_key_id     $AWS_CREDENTIALS_USR
                    aws configure set aws_secret_access_key $AWS_CREDENTIALS_PSW
                    aws configure set region                $AWS_REGION

                    aws ecr get-login-password --region $AWS_REGION | \
                        docker login --username AWS --password-stdin $ECR_REGISTRY
                '''

                parallel(
                    'config-server':       { jibBuild('infrastructure/config-server',       'config-server') },
                    'discovery-server':    { jibBuild('infrastructure/discovery-server',    'discovery-server') },
                    'api-gateway':         { jibBuild('infrastructure/api-gateway',         'api-gateway') },
                    'user-service':        { jibBuild('services/user-service',              'user-service') },
                    'product-service':     { jibBuild('services/product-service',           'product-service') },
                    'shopping-cart':       { jibBuild('services/shopping-cart-service',     'shopping-cart-service') },
                    'order-service':       { jibBuild('services/order-service',             'order-service') },
                    'stock-service':       { jibBuild('services/stock-service',             'stock-service') },
                    'payment-service':     { jibBuild('services/payment-service',           'payment-service') },
                    'cargo-service':       { jibBuild('services/cargo-service',             'cargo-service') },
                    'notification':        { jibBuild('services/notification-service',      'notification-service') },
                    'search-service':      { jibBuild('ai-services/search-service',         'search-service') },
                    'recommendation':      { jibBuild('ai-services/recommendation-service', 'recommendation-service') },
                    'assistant-service':   { jibBuild('ai-services/assistant-service',      'assistant-service') }
                )
            }
        }

        stage('Deploy → EB') {
            when {
                branch 'main'
            }
            environment {
                EB_APP  = credentials('eb-application-name')
                EB_ENV  = credentials('eb-environment-name')
                EB_S3   = credentials('eb-s3-bucket')
            }
            steps {
                sh '''
                    # Ortam değişkenlerini Dockerrun şablonuna yerleştir
                    export CONFIG_SERVER_IMAGE=$ECR_REGISTRY/config-server:$IMAGE_TAG
                    export DISCOVERY_IMAGE=$ECR_REGISTRY/discovery-server:$IMAGE_TAG
                    export GATEWAY_IMAGE=$ECR_REGISTRY/api-gateway:$IMAGE_TAG
                    export USER_IMAGE=$ECR_REGISTRY/user-service:$IMAGE_TAG
                    export PRODUCT_IMAGE=$ECR_REGISTRY/product-service:$IMAGE_TAG
                    export CART_IMAGE=$ECR_REGISTRY/shopping-cart-service:$IMAGE_TAG
                    export ORDER_IMAGE=$ECR_REGISTRY/order-service:$IMAGE_TAG
                    export STOCK_IMAGE=$ECR_REGISTRY/stock-service:$IMAGE_TAG
                    export PAYMENT_IMAGE=$ECR_REGISTRY/payment-service:$IMAGE_TAG
                    export CARGO_IMAGE=$ECR_REGISTRY/cargo-service:$IMAGE_TAG
                    export NOTIFICATION_IMAGE=$ECR_REGISTRY/notification-service:$IMAGE_TAG
                    export SEARCH_IMAGE=$ECR_REGISTRY/search-service:$IMAGE_TAG
                    export RECOMMENDATION_IMAGE=$ECR_REGISTRY/recommendation-service:$IMAGE_TAG
                    export ASSISTANT_IMAGE=$ECR_REGISTRY/assistant-service:$IMAGE_TAG

                    envsubst < infrastructure/aws/Dockerrun.aws.json.tpl > Dockerrun.aws.json

                    VERSION_LABEL="jenkins-${IMAGE_TAG}-$(date +%s)"
                    BUNDLE="ecommerce-${VERSION_LABEL}.zip"

                    zip $BUNDLE Dockerrun.aws.json infrastructure/aws/.ebextensions/ -r

                    aws s3 cp $BUNDLE s3://$EB_S3/$BUNDLE

                    aws elasticbeanstalk create-application-version \
                        --application-name  "$EB_APP" \
                        --version-label     "$VERSION_LABEL" \
                        --source-bundle     S3Bucket="$EB_S3",S3Key="$BUNDLE"

                    aws elasticbeanstalk update-environment \
                        --application-name  "$EB_APP" \
                        --environment-name  "$EB_ENV" \
                        --version-label     "$VERSION_LABEL"

                    aws elasticbeanstalk wait environment-updated \
                        --application-name  "$EB_APP" \
                        --environment-names "$EB_ENV"
                '''
            }
        }
    }

    post {
        success {
            slackNotify('Deploy başarılı', 'good')
        }
        failure {
            slackNotify('Pipeline başarısız', 'danger')
        }
        unstable {
            slackNotify('Build unstable (test hataları var)', 'warning')
        }
    }
}

def jibBuild(String modulePath, String imageName) {
    sh """
        ./mvnw -pl ${modulePath} jib:build \\
            --batch-mode \\
            -DskipTests \\
            -Dimage=${ECR_REGISTRY}/${imageName}:${IMAGE_TAG} \\
            -Djib.to.tags="${IMAGE_TAG},latest" \\
            -Djib.to.auth.username=AWS \\
            "-Djib.to.auth.password=\$(aws ecr get-login-password --region ${AWS_REGION})"
    """
}


def slackNotify(String message, String color) {
    def payload = """
    {
      "attachments": [{
        "color":  "${color}",
        "text":   "${message} — *${env.JOB_NAME}* #${env.BUILD_NUMBER}",
        "footer": "Branch: ${env.BRANCH_NAME} | Commit: ${env.GIT_COMMIT[0..7]}"
      }]
    }
    """
    sh "curl -s -X POST -H 'Content-type: application/json' --data '${payload}' ${SLACK_WEBHOOK}"
}
