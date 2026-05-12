import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Footer } from '../../../shared/components/footer/footer';
import { HotelService } from '../../../core/services/hotel.service';
import { RoomResponse } from '../../../core/models/hotel.model';

@Component({
  selector: 'app-manager-rooms',
  imports: [FormsModule, ReactiveFormsModule, DecimalPipe, Navbar, Footer],
  templateUrl: './manager-rooms.html',
  styleUrl: './manager-rooms.css'
})
export class ManagerRooms implements OnInit {
  private hotelSvc = inject(HotelService);
  private fb       = inject(FormBuilder);

  hotelId    = signal(0);
  rooms      = signal<RoomResponse[]>([]);
  filtered   = signal<RoomResponse[]>([]);
  loading    = signal(true);
  showModal  = signal(false);
  editingRoom = signal<RoomResponse | null>(null);
  saving     = signal(false);
  search    = '';   // plain property — compatible with ngModel / input binding

  occupied   = signal(0);
  available  = signal(0);
  totalRooms = signal(0);
  avgPrice   = signal(0);

  form = this.fb.group({
    type:         ['', Validators.required],
    price:        [0, [Validators.required, Validators.min(1)]],
    availability: [true],
    features:     [''],
    imageUrl:     ['']
  });

  ngOnInit() {
    this.hotelSvc.getMyHotel().subscribe(h => {
      this.hotelId.set(h.hotelId);
      this.loadRooms(h.hotelId);
    });
  }

  loadRooms(hid: number) {
    this.loading.set(true);
    this.hotelSvc.getRoomsForHotel(hid).subscribe(r => {
      this.rooms.set(r);
      this.filtered.set(r);
      this.available.set(r.filter(x => x.availability).length);
      this.occupied.set(r.filter(x => !x.availability).length);
      this.totalRooms.set(r.length);
      this.avgPrice.set(r.length ? Math.round(r.reduce((s, x) => s + Number(x.price), 0) / r.length) : 0);
      this.loading.set(false);
    });
  }

  onSearch() {
    const q = this.search.toLowerCase();
    this.filtered.set(!q ? this.rooms() : this.rooms().filter(r => r.type.toLowerCase().includes(q)));
  }

  features(str: string): string[] {
    return (str || '').split(',').map(s => s.trim()).filter(Boolean);
  }

  openAdd() {
    this.editingRoom.set(null);
    this.form.reset({ availability: true, price: 0 });
    this.showModal.set(true);
  }

  openEdit(r: RoomResponse) {
    this.editingRoom.set(r);
    this.form.patchValue({ type: r.type, price: Number(r.price), availability: r.availability, features: r.features, imageUrl: r.imageUrl });
    this.showModal.set(true);
  }

  save() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const req = this.form.value as any;
    const hid = this.hotelId();
    const editing = this.editingRoom();
    const obs = editing
      ? this.hotelSvc.updateRoom(hid, editing.roomId, req)
      : this.hotelSvc.addRoom(hid, req);
    obs.subscribe({
      next: () => { this.saving.set(false); this.showModal.set(false); this.loadRooms(hid); },
      error: () => this.saving.set(false)
    });
  }

  imgError(e: Event) {
    (e.target as HTMLImageElement).src = 'https://placehold.co/400x260/1a237e/fff?text=Room';
  }
}
