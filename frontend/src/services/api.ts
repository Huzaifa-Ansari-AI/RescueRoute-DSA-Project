import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";
const TOKEN_KEY = "rescueroute_token";

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

// Request interceptor: Attach JWT bearer token to authenticated requests
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: format errors and handle session expiration
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    let message = "Network error: Unable to reach RescueRoute server. Please check your connection.";

    if (error.response?.data?.message) {
      message = error.response.data.message;
    } else if (error.message) {
      message = error.message;
    }

    // If 401 occurs on an authenticated route other than login/register, clear invalid local credentials
    if (
      error.response?.status === 401 &&
      !error.config?.url?.includes("/auth/login") &&
      !error.config?.url?.includes("/auth/register")
    ) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem("rescueroute_user");
    }

    return Promise.reject(new Error(message));
  }
);

export default apiClient;
