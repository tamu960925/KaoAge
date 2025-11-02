# Self-Checkout Privacy Assurance

- All age/gender inference happens on-device; kiosk network radios remain disabled.
- No face imagery, intermediate tensors, or serialized results persist beyond the active session.
- BestShot recommendations only emit metadata (timestamps, reasons) without pixel buffers.
- Operators must clear session context when shoppers abandon the flow to prevent residual data.
