/** @type {import('tailwindcss').Config} */
export default {
  content: [
    '../resources/templates/**/*.html',
    '../resources/static/js/**/*.js',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ["'IBM Plex Sans KR'", "'IBM Plex Sans'", 'system-ui', 'sans-serif'],
      },
      colors: {
        // Carbon semantic tokens via CSS variables
        cds: {
          background:          'var(--cds-background)',
          'background-hover':  'var(--cds-background-hover)',
          'layer-01':          'var(--cds-layer-01)',
          'layer-02':          'var(--cds-layer-02)',
          'field-01':          'var(--cds-field-01)',
          'field-02':          'var(--cds-field-02)',
          'border-subtle':     'var(--cds-border-subtle-00)',
          'border-strong':     'var(--cds-border-strong-01)',
          'border-interactive': 'var(--cds-border-interactive)',
          'text-primary':      'var(--cds-text-primary)',
          'text-secondary':    'var(--cds-text-secondary)',
          'text-placeholder':  'var(--cds-text-placeholder)',
          'text-helper':       'var(--cds-text-helper)',
          'text-error':        'var(--cds-text-error)',
          'text-on-color':     'var(--cds-text-on-color)',
          'link-primary':      'var(--cds-link-primary)',
          'icon-primary':      'var(--cds-icon-primary)',
          'icon-secondary':    'var(--cds-icon-secondary)',
          'interactive':       'var(--cds-interactive)',
          'focus':             'var(--cds-focus)',
          'overlay':           'var(--cds-overlay)',
        },
        // Keep direct Carbon palette for non-variable usage
        primary:     { DEFAULT: '#0f62fe', hover: '#0353e9' },
        secondary:   { DEFAULT: '#393939', hover: '#4c4c4c' },
        destructive: { DEFAULT: '#da1e28', hover: '#ba1b23' },
        success:     { DEFAULT: '#24a148', hover: '#198038' },
        warning:     { DEFAULT: '#f1c21b', hover: '#d2a106' },
        surface:     { DEFAULT: '#ffffff', secondary: '#f4f4f4' },
        // Sidebar colors
        sidebar: {
          bg:            'var(--cds-sidebar-bg)',
          text:          'var(--cds-sidebar-text)',
          'text-active': 'var(--cds-sidebar-text-active)',
          'item-hover':  'var(--cds-sidebar-item-hover)',
          'item-active': 'var(--cds-sidebar-item-active)',
        },
      },
      spacing: {
        'cds-01': 'var(--cds-spacing-01)',
        'cds-02': 'var(--cds-spacing-02)',
        'cds-03': 'var(--cds-spacing-03)',
        'cds-04': 'var(--cds-spacing-04)',
        'cds-05': 'var(--cds-spacing-05)',
        'cds-06': 'var(--cds-spacing-06)',
        'cds-07': 'var(--cds-spacing-07)',
        'cds-08': 'var(--cds-spacing-08)',
        'cds-09': 'var(--cds-spacing-09)',
        compact:   '0.5rem',
        regular:   '1rem',
        generous:  '1.5rem',
        spacious:  '2rem',
      },
      fontSize: {
        'cds-label':      ['var(--cds-label-01-size)', { lineHeight: '1.34' }],
        'cds-helper':     ['var(--cds-helper-text-01-size)', { lineHeight: '1.34' }],
        'cds-body-01':    ['var(--cds-body-01-size)', { lineHeight: '1.43' }],
        'cds-body-02':    ['var(--cds-body-02-size)', { lineHeight: '1.5' }],
        'cds-heading-01': ['var(--cds-heading-01-size)', { lineHeight: '1.43', fontWeight: '600' }],
        'cds-heading-02': ['var(--cds-heading-02-size)', { lineHeight: '1.5', fontWeight: '600' }],
        'cds-heading-03': ['var(--cds-heading-03-size)', { lineHeight: '1.4', fontWeight: '600' }],
        'cds-heading-04': ['var(--cds-heading-04-size)', { lineHeight: '1.29', fontWeight: '600' }],
      },
      boxShadow: {
        'cds': 'var(--cds-shadow)',
      },
      borderRadius: {
        'cds': '0',  // Carbon has no border radius by default
      },
    },
  },
  plugins: [],
};
