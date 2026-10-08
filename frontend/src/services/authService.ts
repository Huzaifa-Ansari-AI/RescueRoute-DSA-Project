import apiClient from "./api";
import type { ApiResponse } from "../types/facility";
import type { AuthResponse, LoginCredentials, RegisterData, User } from "../types/auth";

const TOKEN_KEY = "rescueroute_token";
const USER_KEY = "rescueroute_user";

export const authService = {
  /**
   * Registers a new user account.
   */
  async register(data: RegisterData): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/register", data);
    const authData = response.data.data;
    if (authData?.token) {
      this.setToken(authData.token);
      this.setUser(authData.user);
    }
    return authData;
  },

  /**
   * Authenticates user with email and password.
   */
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/login", credentials);
    const authData = response.data.data;
    if (authData?.token) {
      this.setToken(authData.token);
      this.setUser(authData.user);
    }
    return authData;
  },

  /**
   * Fetches current authenticated user profile using active JWT.
   */
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>("/auth/me");
    const user = response.data.data;
    if (user) {
      this.setUser(user);
    }
    return user;
  },

  /**
   * Logs out the user by clearing local authentication storage.
   */
  logout(): void {
    this.removeToken();
    this.removeUser();
  },

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  },

  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY);
  },

  getUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  },

  setUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },

  removeUser(): void {
    localStorage.removeItem(USER_KEY);
  },

  isAuthenticated(): boolean {
    return Boolean(this.getToken());
  },
};

export default authService;
