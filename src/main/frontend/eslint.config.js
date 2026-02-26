import globals from 'globals';

export default [{
  files: ['../resources/static/js/**/*.js'],
  languageOptions: {
    globals: {
      ...globals.browser,
      ...globals.jquery,
    },
  },
  rules: {
    'no-var':                'error',
    'no-implicit-globals':   'error',
    'eqeqeq':               'error',
    'prefer-const':          'warn',
    'no-restricted-globals': ['error', { name: 'fetch', message: 'Use $.ajax() instead.' }],
    'no-restricted-syntax':  ['warn',
      { selector: "CallExpression[callee.property.name='addEventListener']",
        message: 'Use jQuery .off().on() instead.' }
    ],
  },
}];
