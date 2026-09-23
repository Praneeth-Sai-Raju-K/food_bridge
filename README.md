# 🌱 Food Bridge

**Food Bridge** is an Android application that connects people with surplus food — restaurants, canteens, event organizers, or individuals — with local NGOs and volunteers who can redistribute it to those in need. Built as a full-featured mobile app with a live backend, real-time data sync, and role-based access control.

## 📱 Features

- **Authentication & Roles** — Sign up as a Donor, NGO, or Volunteer with name, phone, and email. Role-based UI ensures Donors post donations while NGOs/Volunteers claim them.
- **Real-Time Donation Feed** — Live-updating feed powered by Firestore snapshot listeners; no manual refresh needed.
- **Search, Filter & Sort** — Search by food name or location, filter by category (Veg / Non-Veg / Bakery / Cooked), and sort by newest or soonest-expiring.
- **GPS Location Capture** — Donors can auto-fill their pickup location using the device's GPS, with reverse geocoding to a readable address.
- **Embedded Maps** — Donation locations are shown on an interactive OpenStreetMap-based map, with a fallback to open Google Maps directly.
- **Claim & Coordinate** — NGOs/Volunteers claim a donation with one tap; donor and claimer phone numbers are exchanged automatically to coordinate pickup.
- **Local Notifications** — Donors are notified the moment their donation is claimed.
- **Ratings & Impact Stats** — Claimers rate their pickup experience; Donors see their average rating and total donations on their profile.
- **Profile Dashboard** — Role badge, live stats, donation/claim history, and account management (logout, delete unclaimed donations).
- **Dark Mode** — Full day/night theme support.
- **Forgot Password** — Real password-reset email flow via Firebase Authentication.

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Android Views (XML layouts)
- **Backend:** Firebase Authentication, Cloud Firestore (real-time NoSQL database)
- **Maps:** osmdroid (OpenStreetMap) with Android's built-in Geocoder
- **Architecture:** Activity-based navigation with Firestore snapshot listeners for live data

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable version)
- A Firebase project with **Authentication** (Email/Password) and **Firestore** enabled

### Setup
1. Clone this repository
   ```
   git clone https://github.com/Praneeth-Sai-Raju-K/food_bridge.git
   ```
2. Open the project in Android Studio
3. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com), add an Android app with package name `com.praneeth.foodbridge`, and download your own `google-services.json` into the `app/` folder
4. Enable **Email/Password** sign-in under Firebase Authentication
5. Create a **Firestore Database** in test mode
6. Build and run on an emulator or physical device

## 👥 User Roles

| Role | Can Post Donations | Can Claim Donations |
|------|:---:|:---:|
| Donor | ✅ | ❌ |
| NGO | ❌ | ✅ |
| Volunteer | ❌ | ✅ |

## 📄 License

This project was built as an academic mobile application development project.

