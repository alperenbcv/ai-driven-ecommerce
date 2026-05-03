{
  "AWSEBDockerrunVersion": 2,
  "containerDefinitions": [
    {
      "name": "rabbitmq",
      "image": "rabbitmq:3.13-management-alpine",
      "essential": true,
      "memory": 256,
      "portMappings": [
        { "hostPort": 5672,  "containerPort": 5672  },
        { "hostPort": 15672, "containerPort": 15672 }
      ],
      "environment": [
        { "name": "RABBITMQ_DEFAULT_USER", "value": "ecommerce" },
        { "name": "RABBITMQ_DEFAULT_PASS", "value": "ecommerce123" }
      ]
    },
    {
      "name": "neo4j",
      "image": "neo4j:5.24-community",
      "essential": false,
      "memory": 512,
      "portMappings": [
        { "hostPort": 7474, "containerPort": 7474 },
        { "hostPort": 7687, "containerPort": 7687 }
      ],
      "environment": [
        { "name": "NEO4J_AUTH",    "value": "neo4j/ecommerce123" },
        { "name": "NEO4J_PLUGINS", "value": "[\"apoc\"]" }
      ],
      "mountPoints": [
        { "sourceVolume": "neo4j-data", "containerPath": "/data" }
      ]
    },
    {
      "name": "config-server",
      "image": "${CONFIG_SERVER_IMAGE}",
      "essential": true,
      "memory": 256,
      "portMappings": [{ "hostPort": 8888, "containerPort": 8888 }],
      "links": ["rabbitmq"]
    },
    {
      "name": "discovery-server",
      "image": "${DISCOVERY_IMAGE}",
      "essential": true,
      "memory": 256,
      "portMappings": [{ "hostPort": 8761, "containerPort": 8761 }],
      "links": ["config-server"]
    },
    {
      "name": "api-gateway",
      "image": "${GATEWAY_IMAGE}",
      "essential": true,
      "memory": 256,
      "portMappings": [{ "hostPort": 80, "containerPort": 8763 }],
      "links": ["config-server", "discovery-server"]
    },
    {
      "name": "user-service",
      "image": "${USER_IMAGE}",
      "essential": true,
      "memory": 256,
      "portMappings": [{ "hostPort": 8081, "containerPort": 8081 }],
      "links": ["config-server", "discovery-server", "rabbitmq"]
    },
    {
      "name": "product-service",
      "image": "${PRODUCT_IMAGE}",
      "essential": true,
      "memory": 256,
      "portMappings": [{ "hostPort": 8082, "containerPort": 8082 }],
      "links": ["config-server", "discovery-server", "rabbitmq"]
    },
    {
      "name": "shopping-cart-service",
      "image": "${CART_IMAGE}",
      "essential": false,
      "memory": 256,
      "portMappings": [{ "hostPort": 8083, "containerPort": 8083 }],
      "links": ["config-server", "discovery-server"]
    },
    {
      "name": "order-service",
      "image": "${ORDER_IMAGE}",
      "essential": true,
      "memory": 256,
      "portMappings": [{ "hostPort": 8084, "containerPort": 8084 }],
      "links": ["config-server", "discovery-server", "rabbitmq"]
    },
    {
      "name": "stock-service",
      "image": "${STOCK_IMAGE}",
      "essential": false,
      "memory": 256,
      "portMappings": [{ "hostPort": 8085, "containerPort": 8085 }],
      "links": ["config-server", "discovery-server", "rabbitmq"]
    },
    {
      "name": "payment-service",
      "image": "${PAYMENT_IMAGE}",
      "essential": true,
      "memory": 256,
      "portMappings": [{ "hostPort": 8086, "containerPort": 8086 }],
      "links": ["config-server", "discovery-server", "rabbitmq"]
    },
    {
      "name": "cargo-service",
      "image": "${CARGO_IMAGE}",
      "essential": false,
      "memory": 256,
      "portMappings": [{ "hostPort": 8087, "containerPort": 8087 }],
      "links": ["config-server", "discovery-server", "rabbitmq"]
    },
    {
      "name": "notification-service",
      "image": "${NOTIFICATION_IMAGE}",
      "essential": false,
      "memory": 256,
      "portMappings": [{ "hostPort": 8088, "containerPort": 8088 }],
      "links": ["config-server", "discovery-server", "rabbitmq"]
    },
    {
      "name": "search-service",
      "image": "${SEARCH_IMAGE}",
      "essential": false,
      "memory": 256,
      "portMappings": [{ "hostPort": 8089, "containerPort": 8089 }],
      "links": ["config-server", "discovery-server"]
    },
    {
      "name": "recommendation-service",
      "image": "${RECOMMENDATION_IMAGE}",
      "essential": false,
      "memory": 256,
      "portMappings": [{ "hostPort": 8090, "containerPort": 8090 }],
      "links": ["config-server", "discovery-server", "neo4j"]
    },
    {
      "name": "assistant-service",
      "image": "${ASSISTANT_IMAGE}",
      "essential": false,
      "memory": 256,
      "portMappings": [{ "hostPort": 8092, "containerPort": 8092 }],
      "links": ["config-server", "discovery-server"]
    }
  ],
  "volumes": [
    {
      "name": "neo4j-data",
      "host": { "sourcePath": "/var/app/neo4j-data" }
    }
  ]
}
