import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class Landing {
  year = new Date().getFullYear();

  stats = [
    { value: '500+',  label: 'Hotels Listed'   },
    { value: '50K+',  label: 'Happy Guests'    },
    { value: '120+',  label: 'Cities Covered'  },
    { value: '4.9',   label: 'Average Rating'  }
  ];

  // Feature cards are rendered with static SVGs in the template
  // to avoid Angular's innerHTML sanitizer stripping SVG content.

  steps = [
    {
      num: '01',
      title: 'Search',
      desc: 'Browse our curated collection by name, location, or amenity to find your ideal property.'
    },
    {
      num: '02',
      title: 'Book',
      desc: 'Select your room, choose dates, and confirm your reservation instantly with zero friction.'
    },
    {
      num: '03',
      title: 'Earn',
      desc: 'Every booking earns loyalty points you can redeem for discounts on your next adventure.'
    }
  ];

  managerFeatures = [
    'List and manage your rooms with rich details',
    'Accept and track bookings in real time',
    'Read and respond to guest reviews',
    'Monitor occupancy from a clean dashboard',
    'Get listed to thousands of verified guests'
  ];

  testimonials = [
    {
      quote: 'HotelVerse made our anniversary trip flawless. Booking was instant and the loyalty points got us a discount on our very next stay!',
      name: 'Priya Sharma',
      location: 'Mumbai',
      rating: 5,
      initials: 'PS',
      color: '#7986cb'
    },
    {
      quote: 'As a hotel manager, the platform is incredibly easy to use. We saw a 40% increase in bookings within the first month of listing.',
      name: 'Rahul Mehta',
      location: 'Bengaluru',
      rating: 5,
      initials: 'RM',
      color: '#4db6ac'
    },
    {
      quote: 'Clean interface, great hotels, and the rewards program is genuinely useful. This is the only booking platform I use now.',
      name: 'Kavitha Nair',
      location: 'Chennai',
      rating: 5,
      initials: 'KN',
      color: '#ff8a65'
    }
  ];

  stars(n: number): number[] {
    return Array(n).fill(0);
  }
}
