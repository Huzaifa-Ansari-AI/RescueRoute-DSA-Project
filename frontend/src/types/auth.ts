export type UserRole = "USER" | "PROVIDER" | "ADMIN";

export interface User {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  isActive: boolean;
  createdAt?: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterData {
  fullName: string;
  email: string;
  password: string;
  role?: UserRole;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}
