# Cashier Compliance Playbook

- Keep device offline during demos to reinforce on-device analysis.
- Proceed when age bracket is `ADULT` with confidence ≥ 0.8; otherwise request ID verification.
- If gender confidence returns `UNDETERMINED`, ask shopper politely to adjust pose or remove obstructive accessories.
- To protect privacy, clear the session after each checkout using `FaceInsightsSession.clear()` (forthcoming) or restart the capture flow.
