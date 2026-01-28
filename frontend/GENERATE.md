# MatrosDMS Frontend – NPM Manual

This is the Vue 3 + TypeScript frontend for **MatrosDMS**.  
It uses a **Schema-First** approach where TypeScript types and enums are auto-generated from the backend’s OpenAPI definition.

---

## 📋 Prerequisites

- **Node.js**: v18 or higher recommended
- **Backend**:  
  The Spring Boot backend should be running at  
  http://localhost:9090  
  (or the OpenAPI spec must be available at ../spec/openapi.json)

---

## 🚀 Getting Started

### 1. Install Dependencies

Installs all required packages defined in package.json.

npm install

---

### 2. Start Development Server

Starts the Vite server with Hot Module Replacement (HMR).

npm run dev

Local URL: http://localhost:5173  
Proxy: API requests (/api/*) are automatically proxied to http://localhost:9090

---

## ⚙️ Code Generation (Model Driven)

We do not manually write Types or Enums for API objects.  
They are generated from openapi.json to ensure strict consistency between backend and frontend.

### The “Magic” Command

Run this command whenever the backend API changes  
(e.g. new fields, new enum values):

npm run generate

What it does:  
Runs gen:api followed by gen:enums.

---

## 🧩 Granular Commands

### 1. Generate TypeScript Interfaces

Reads openapi.json and generates strict TypeScript interfaces (models).

npm run gen:api

Underlying command:

npx openapi-typescript ../spec/openapi.json -o ./src/types/schema.ts

Input: ../spec/openapi.json  
Output: src/types/schema.ts  
Usage: API client typing (e.g. components['schemas']['MItem'])

---

### 2. Generate Enums & Lists

Reads openapi.json, finds all enums (e.g. EStage, ERole), and generates
individual TypeScript files containing:
- the enum
- a list (for dropdowns)
- labels

npm run update:enum

Input: ../spec/openapi.json  
Output:
- src/enums/*.ts
- src/enums/index.ts

Usage example:

import { EStage, EStageList } from '@/enums';

---

## 📦 Production & Building

### Build for Production

Compiles the application into static files optimized for production.

npm run build

Output: ./dist  
Validation: Runs vue-tsc to check for type errors before building

---

### Preview Production Build

Locally preview the production build before deploying.

npm run preview

---

## 🧪 Testing (Playwright)

### Run End-to-End Tests

Runs all E2E tests in headless mode.

npm run test:e2e

---

### Run Tests with UI

Opens the Playwright interactive UI to run tests, inspect traces, and debug.

npm run test:e2e:ui

---

## 📂 Project Structure Overview

frontend/
├── scripts/
│   └── gen-enums.js       # Script logic for generating enums
├── src/
│   ├── api/               # API client configuration
│   ├── components/        # Vue components
│   ├── enums/             # AUTO-GENERATED enums (do not edit)
│   ├── services/          # Business logic wrapping the API
│   ├── stores/            # Pinia state stores
│   ├── types/
│   │   └── schema.ts      # AUTO-GENERATED OpenAPI types
│   ├── App.vue
│   └── main.ts
├── package.json           # NPM scripts
├── vite.config.js         # Vite & proxy configuration
└── README.md              # This file
