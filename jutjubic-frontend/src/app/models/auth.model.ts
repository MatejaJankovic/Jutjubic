export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  address: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface MessageResponse {
  message: string;
  success: boolean;
}

export interface ErrorResponse {
  message: string;
  timestamp: string;
  path: string;
}

export interface ValidationErrorResponse extends ErrorResponse {
  errors: { [key: string]: string };
}

