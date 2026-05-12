export type Role = 'ROLE_GUEST' | 'ROLE_HOTEL_MANAGER' | 'ROLE_ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
  contactNumber: string;
}

export interface JwtResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface User {
  userId: number;
  name: string;
  email: string;
  role: Role;
  contactNumber: string;
}
