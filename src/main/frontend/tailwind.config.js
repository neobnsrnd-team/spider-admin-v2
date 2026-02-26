/** @type {import('tailwindcss').Config} */
export default {
  content: [
    '../resources/templates/**/*.html',
    '../resources/static/js/**/*.js',
  ],
  theme: {
    extend: {
      colors: {
        primary:     { DEFAULT: '#0f62fe', hover: '#0353e9' },
        secondary:   { DEFAULT: '#393939', hover: '#4c4c4c' },
        destructive: { DEFAULT: '#da1e28', hover: '#ba1b23' },
        success:     { DEFAULT: '#24a148', hover: '#198038' },
        warning:     { DEFAULT: '#f1c21b', hover: '#d2a106' },
        surface:     { DEFAULT: '#ffffff', secondary: '#f4f4f4' },
      },
      spacing: {
        compact:   '0.5rem',
        regular:   '1rem',
        generous:  '1.5rem',
        spacious:  '2rem',
      },
    },
  },
  plugins: [],
};
