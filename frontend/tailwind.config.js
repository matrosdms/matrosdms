/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    './pages/**/*.{js,jsx,vue}',
    './components/**/*.{js,jsx,vue}',
    './app/**/*.{js,jsx,vue}',
    './src/**/*.{js,jsx,vue}',
  ],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: { "2xl": "1400px" },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        // Semantic Tokens
        primary: {
          DEFAULT: "#2563eb", // blue-600
          foreground: "#ffffff",
          hover: "#1d4ed8", // blue-700
          light: "#eff6ff", // blue-50
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        destructive: {
          DEFAULT: "#ef4444", // red-500
          foreground: "#ffffff",
          hover: "#dc2626", // red-600
          light: "#fef2f2", // red-50
        },
        success: {
          DEFAULT: "#10b981", // emerald-500
          foreground: "#ffffff",
          light: "#ecfdf5", // emerald-50
        },
        warning: {
          DEFAULT: "#f59e0b", // amber-500
          foreground: "#ffffff",
          light: "#fffbeb", // amber-50
        },
      },
      borderRadius: {
        lg: "0.5rem",
        md: "0.375rem",
        sm: "0.25rem",
      },
    },
  },
  plugins: [],
}