# Buddy AI Gateway Setup

Buddy already supports an OpenAI-compatible Chat Completions endpoint. A separate router is optional; it is not bundled into the Android APK.

## Recommended gateway: OmniRoute

OmniRoute provides an OpenAI-compatible API surface and can route requests to providers/models configured in its dashboard. It is a server process, so Buddy cannot use a desktop-only `localhost` address when the app is running on the phone.

Repository: https://github.com/Gmagl/omniroute

## Configure Buddy

1. Deploy/run OmniRoute on a trusted machine or hosted server and secure it with HTTPS. Do not expose an unauthenticated gateway to the public internet.
2. In the OmniRoute dashboard, connect at least one provider and create a dedicated endpoint API key. Configure a fallback combo if desired.
3. In Buddy → AI Brain Settings, enter:
   - API endpoint: `https://YOUR-HOST/v1/chat/completions`
   - Model name: the exact OpenAI-compatible model ID shown by your OmniRoute endpoint/dashboard.
   - API key: the dedicated OmniRoute endpoint key.
4. Tap SAVE AI SETTINGS and test with a harmless question.

Replace `YOUR-HOST` with the real HTTPS hostname. Do not paste example values literally. Do not use `http://localhost:20128` on the phone: localhost refers to the phone itself, not your laptop. Buddy currently enforces HTTPS endpoints.

## Provider alternatives

Buddy's existing single-provider settings can also connect directly to any provider that offers an OpenAI-compatible Chat Completions endpoint. Use that provider's official HTTPS endpoint, exact model ID, and personal API key. Gemini's OpenAI-compatible interface and providers such as Groq may be configured this way when their current API documentation supports the selected model.

## Important limits and security

- “Free” means subject to the provider's changing rate limits, model availability, and terms; it does not mean unlimited.
- Automatic fallback is performed by the gateway only when you configure a fallback route there. Buddy itself currently stores one endpoint, one model ID, and one API key; it does not yet implement multi-provider fallback locally.
- Never commit provider keys or gateway keys to this repository, screenshots, or chat messages.
- Use a private gateway with authentication and HTTPS. A public unauthenticated proxy can expose your account and incur unexpected usage.
- Buddy sends user prompts to the selected AI service when online AI is configured. Keep sensitive personal, payment, banking, password, and private-media information out of prompts.
- Offline local commands continue to work without a configured AI gateway. Online AI requires internet access and a reachable HTTPS endpoint.
