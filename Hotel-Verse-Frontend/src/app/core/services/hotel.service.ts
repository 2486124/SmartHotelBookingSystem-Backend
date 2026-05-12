import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HotelRequest, HotelResponse, RoomRequest, RoomResponse } from '../models/hotel.model';

@Injectable({ providedIn: 'root' })
export class HotelService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // ── Guest / Public ──────────────────────────────────────────
  getApprovedHotels(): Observable<HotelResponse[]> {
    return this.http.get<HotelResponse[]>(`${this.base}/api/hotels/approved-hotels`);
  }

  getHotelById(id: number): Observable<HotelResponse> {
    return this.http.get<HotelResponse>(`${this.base}/api/hotels/${id}`);
  }

  searchHotels(name?: string, location?: string): Observable<HotelResponse[]> {
    let params = new HttpParams();
    if (name)     params = params.set('name', name);
    if (location) params = params.set('location', location);
    return this.http.get<HotelResponse[]>(`${this.base}/api/hotels/search`, { params });
  }

  getAvailableRooms(hotelId: number, checkIn: string, checkOut: string, roomType?: string): Observable<RoomResponse[]> {
    let params = new HttpParams()
      .set('hotelId', hotelId)
      .set('checkIn', checkIn)
      .set('checkOut', checkOut);
    if (roomType) params = params.set('roomType', roomType);
    return this.http.get<RoomResponse[]>(`${this.base}/api/hotels/rooms/available`, { params });
  }

  getRoomsForHotel(hotelId: number): Observable<RoomResponse[]> {
    return this.http.get<RoomResponse[]>(`${this.base}/api/hotels/${hotelId}/rooms`);
  }

  // ── Manager ─────────────────────────────────────────────────
  getMyHotel(): Observable<HotelResponse> {
    return this.http.get<HotelResponse>(`${this.base}/api/hotels/manager`);
  }

  createHotel(req: HotelRequest): Observable<HotelResponse> {
    return this.http.post<HotelResponse>(`${this.base}/api/hotels`, req);
  }

  updateHotel(id: number, req: HotelRequest): Observable<HotelResponse> {
    return this.http.put<HotelResponse>(`${this.base}/api/hotels/${id}`, req);
  }

  deleteMyHotel(): Observable<string> {
    return this.http.delete(`${this.base}/api/hotels/delete-hotel`, { responseType: 'text' });
  }

  addRoom(hotelId: number, req: RoomRequest): Observable<RoomResponse> {
    return this.http.post<RoomResponse>(`${this.base}/api/hotels/${hotelId}/rooms`, req);
  }

  updateRoom(hotelId: number, roomId: number, req: RoomRequest): Observable<RoomResponse> {
    return this.http.put<RoomResponse>(`${this.base}/api/hotels/${hotelId}/rooms/${roomId}`, req);
  }

  deleteRoom(hotelId: number, roomId: number): Observable<string> {
    return this.http.delete(`${this.base}/api/hotels/${hotelId}/rooms/${roomId}`, { responseType: 'text' });
  }

  // ── Admin ────────────────────────────────────────────────────
  approveHotel(id: number): Observable<HotelResponse> {
    return this.http.patch<HotelResponse>(`${this.base}/api/hotels/${id}/approve`, {});
  }

  adminDeleteHotel(id: number): Observable<string> {
    return this.http.delete(`${this.base}/api/hotels/${id}`, { responseType: 'text' });
  }

  adminSearchHotels(name?: string, location?: string): Observable<HotelResponse[]> {
    let params = new HttpParams();
    if (name)     params = params.set('name', name);
    if (location) params = params.set('location', location);
    return this.http.get<HotelResponse[]>(`${this.base}/api/hotels/admin/search`, { params });
  }

  getPendingHotels(): Observable<HotelResponse[]> {
    return this.http.get<HotelResponse[]>(`${this.base}/api/hotels/admin/search`);
  }
}
