export interface HotelResponse {
  hotelId: number;
  name: string;
  location: string;
  amenities: string;
  imageUrl: string;
  rating: number;
  approval: boolean;
  managerId?: number;
}

export interface HotelRequest {
  name: string;
  location: string;
  amenities: string;
  imageUrl: string;
}

export interface RoomResponse {
  roomId: number;
  type: string;
  price: number;
  availability: boolean;
  features: string;
  imageUrl: string;
}

export interface RoomRequest {
  type: string;
  price: number;
  availability: boolean;
  features: string;
  imageUrl: string;
}

export interface RoomFilterRequest {
  hotelId: number;
  roomType?: string;
  checkIn: string;
  checkOut: string;
}
