import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props { children: ReactNode }
interface State { error: Error | null }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('React render hatası:', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="min-h-screen flex items-center justify-center p-8">
          <div className="max-w-lg w-full bg-red-50 border border-red-200 rounded-xl p-6 space-y-3">
            <h2 className="font-bold text-red-700 text-lg">Render Hatası</h2>
            <p className="text-red-600 font-mono text-sm break-all">
              {this.state.error.message}
            </p>
            <pre className="text-xs text-red-500 overflow-auto max-h-48 bg-red-100 rounded p-2">
              {this.state.error.stack}
            </pre>
            <button
              className="px-4 py-2 bg-red-600 text-white rounded text-sm hover:bg-red-700"
              onClick={() => { this.setState({ error: null }); window.location.href = '/' }}
            >
              Ana Sayfaya Dön
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
