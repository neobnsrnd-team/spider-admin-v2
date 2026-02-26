/**
 * SpiderValidation — 폼 검증 헬퍼
 */
(function () {
    'use strict';

    function getErrorEl(field) {
        var el = field.nextElementSibling;
        if (el && el.classList.contains('field-error')) return el;
        el = document.createElement('div');
        el.className = 'field-error';
        Object.assign(el.style, {
            color: 'var(--cds-text-error)',
            fontSize: 'var(--cds-helper-text-01-size, 0.75rem)',
            marginTop: '0.25rem',
            lineHeight: '1.34',
        });
        field.parentNode.insertBefore(el, field.nextSibling);
        return el;
    }

    function clearError(field) {
        field.style.borderColor = '';
        var el = field.nextElementSibling;
        if (el && el.classList.contains('field-error')) {
            el.textContent = '';
            el.style.display = 'none';
        }
    }

    function showError(field, message) {
        field.style.borderColor = 'var(--cds-support-error)';
        var el = getErrorEl(field);
        el.textContent = message;
        el.style.display = 'block';
    }

    function t(key, params) {
        var msg = (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
        if (params) {
            Object.keys(params).forEach(function (k) {
                msg = msg.replace('{' + k + '}', params[k]);
            });
        }
        return msg;
    }

    function validateField(field) {
        var rules = (field.getAttribute('data-validate') || '').split(',').map(function (r) { return r.trim(); });
        var value = field.value.trim();
        var label = field.getAttribute('data-label') || field.name || '';

        for (var i = 0; i < rules.length; i++) {
            var rule = rules[i];

            if (rule === 'required' && !value) {
                return t('validation.required', { field: label });
            }

            var minMatch = rule.match(/^minLength:(\d+)$/);
            if (minMatch && value.length < parseInt(minMatch[1], 10)) {
                return t('validation.minLength', { field: label, min: minMatch[1] });
            }

            var maxMatch = rule.match(/^maxLength:(\d+)$/);
            if (maxMatch && value.length > parseInt(maxMatch[1], 10)) {
                return t('validation.maxLength', { field: label, max: maxMatch[1] });
            }

            if (rule === 'email' && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                return t('validation.email');
            }

            if (rule === 'number' && value && !/^\d+$/.test(value)) {
                return t('validation.number');
            }

            var patternMatch = rule.match(/^pattern:(.+)$/);
            if (patternMatch && value && !new RegExp(patternMatch[1]).test(value)) {
                return t('validation.pattern', { field: label });
            }
        }
        return null;
    }

    window.SpiderValidation = {
        /**
         * Validate all fields within a container
         * @param {Element} container - form or container element
         * @returns {{ valid: boolean, errors: Object }}
         */
        validate: function (container) {
            var fields = container.querySelectorAll('[data-validate]');
            var errors = {};
            var firstError = null;

            fields.forEach(function (field) {
                clearError(field);
                var error = validateField(field);
                if (error) {
                    errors[field.name || field.id] = error;
                    showError(field, error);
                    if (!firstError) firstError = field;
                }
            });

            if (firstError) firstError.focus();

            return {
                valid: Object.keys(errors).length === 0,
                errors: errors,
            };
        },

        /** Clear all validation errors in a container */
        clear: function (container) {
            container.querySelectorAll('[data-validate]').forEach(clearError);
        },

        /** Validate a single field */
        validateField: function (field) {
            clearError(field);
            var error = validateField(field);
            if (error) showError(field, error);
            return !error;
        },
    };
})();
