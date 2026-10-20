# Aether GPT Gateway (optional)

This is the **secure planning endpoint**, not a phone remote-control server.
It receives only the prompt typed into Aether AI, asks an OpenAI model for a
strictly typed plan, rejects unsupported actions, and returns a response for
the Android app to show for approval. The server cannot execute phone actions.

## Deploy on Oracle Ubuntu

Requires Node.js 20+ and a domain with working HTTPS (e.g. Caddy). API usage
is **billed separately from ChatGPT subscriptions**.

1. On the server, install Node 22 or newer.
2. Copy `gateway/` to a private app directory on your server.
3. Set secrets in a restricted local environment file, never in GitHub.
4. Run:
   ```bash
   cd gateway
   export OPENAI_API_KEY='YOUR_SECRET_API_KEY'
   export AETHER_DEVICE_TOKEN="$(openssl rand -hex 32)"
   npm test
   npm start
   ```
5. Configure Caddy for a **dedicated** hostname (replace the example domain):
   ```caddyfile
   ai.your-domain.example {
       reverse_proxy 127.0.0.1:17891
   }
   ```
   Do not expose TCP port 17891 directly; the gateway binds to 127.0.0.1.
6. Enter `https://ai.your-domain.example/v1/plan` and the random device
   token in AetherOS > AI > GPT server connection. The token is NOT your
   OpenAI key, and is encrypted using Android Keystore on the phone.
7. Choose GPT or Hybrid mode. The phone will display all proposed actions
   before executing anything.

## Privacy and safety

- Transport requires HTTPS; app redirects are disabled.
- The OpenAI API key never leaves the server and is not bundled in the APK.
- Prompts are sent to your own server, which forwards them to OpenAI.
- Responses use `store:false`. The gateway itself doesn't log prompts.
- Text actions are allowlisted to SMS/KakaoTalk draft, exact installed app
  launch and safe settings navigation. No shell, payment or package removal.
- Final KakaoTalk/SMS sending always requires manual verification in the
  destination app. AetherOS does not silently send.
- Rate limited: 20 requests per minute across the single-user gateway.
- Model output is a proposal, not instructions the phone trusts.
- The gateway token is a per-device credential: rotate on leak.
- Configure usage budgets and billing alerts in the OpenAI API dashboard.

## Operations

`GET /health` only returns service readiness (no secret values).
`POST /v1/plan` takes `{"prompt":"..."}` with header
`Authorization: Bearer YOUR_DEVICE_TOKEN`.
No general-purpose chat history is retained. No endpoints for remote
screenshots or arbitrary phone control.

Recommended upgrades: properly authenticated per-device sessions, audit trail
with sensitive fields redacted, command disambiguation, per-user quota and
open-source-compatible local models behind the same interface.

Tests: `npm test`.
