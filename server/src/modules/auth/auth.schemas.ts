import { z } from "zod";

export const Email = z.email().max(254);
// NIST SP 800-63B: length matters more than composition rules.
export const NewPassword = z.string().min(10).max(256);
const DeviceName = z.string().trim().min(1).max(100).optional();

export const RegisterBody = z.object({
  email: Email,
  password: NewPassword,
  displayName: z.string().trim().min(1).max(100).optional(),
  locale: z.enum(["en", "ar"]).default("en"),
  deviceName: DeviceName,
});

export const LoginBody = z.object({ email: Email, password: z.string().min(1).max(256), deviceName: DeviceName });
export const RefreshBody = z.object({ refreshToken: z.string().min(20).max(200) });

export const TokenPair = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  tokenType: z.literal("Bearer"),
  expiresIn: z.number().int().describe("Seconds until the access token expires"),
  refreshExpiresIn: z.number().int(),
});

export const User = z.object({
  id: z.uuid(),
  email: z.string(),
  displayName: z.string().nullable(),
  /** Whether the email address has been confirmed (V3 AM05). */
  emailVerified: z.boolean(),
  locale: z.string(),
  createdAt: z.string(),
});

export const AuthResult = z.object({ user: User, tokens: TokenPair });

export const DeviceSession = z.object({
  id: z.uuid(),
  deviceName: z.string().nullable(),
  createdAt: z.string(),
  lastUsedAt: z.string(),
  current: z.boolean(),
});

export const SessionIdParams = z.object({ id: z.uuid() });
