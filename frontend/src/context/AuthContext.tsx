import React, { useEffect, useState, useCallback } from "react";
import type { LoginCredentials, RegisterData, User } from "../types/auth";
import authService from "../services/authService";
import { AuthContext } from "./authContextDef";

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => authService.getUser());
  const [token, setToken] = useState<string | null>(() => authService.getToken());
  const [isLoading, setIsLoading] = useState<boolean>(() => !!authService.getToken());

  // Validate active token with /api/v1/auth/me on mount if token exists
  useEffect(() => {
    let isMounted = true;
    const existingToken = authService.getToken();

    if (!existingToken) {
      return;
    }

    authService
      .getCurrentUser()
      .then((currentUser) => {
        if (isMounted) {
          setUser(currentUser);
          setToken(existingToken);
          setIsLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          authService.logout();
          setUser(null);
          setToken(null);
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const login = useCallback(async (credentials: LoginCredentials) => {
    const authData = await authService.login(credentials);
    setUser(authData.user);
    setToken(authData.token);
  }, []);

  const register = useCallback(async (data: RegisterData) => {
    const authData = await authService.register(data);
    setUser(authData.user);
    setToken(authData.token);
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setUser(null);
    setToken(null);
  }, []);

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    isLoading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthProvider;
