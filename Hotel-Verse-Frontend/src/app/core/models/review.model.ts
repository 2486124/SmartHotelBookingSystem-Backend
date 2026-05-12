export interface ReviewResponseDTO {
  reviewId: number;
  userId: number;
  hotelId: number;
  rating: number;
  comment: string;
  timestamp: string;
  managerResponse?: string;
  responseTimestamp?: string;
  userName?: string;
}

export interface ReviewRequestDTO {
  userId: number;
  hotelId: number;
  rating: number;
  comment: string;
  bookingId: number;
}
