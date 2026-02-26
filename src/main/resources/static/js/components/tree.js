/**
 * SpiderTree -- Carbon TreeView pattern
 *
 * Usage:
 *   const tree = SpiderTree.create(container, {
 *     data: menuTreeData,
 *     labelField: 'menuName',
 *     idField: 'menuId',
 *     childrenField: 'children',
 *     draggable: true,
 *     onSelect: (node) => {},
 *     onDrop: (dragNode, dropNode, position) => {},
 *     renderLabel: (node) => node.menuName
 *   });
 *   tree.setData(newData);
 *   tree.getSelected();
 *   tree.expandAll();
 *   tree.collapseAll();
 *   tree.selectNode(nodeId);
 */
(function () {
    'use strict';

    /* ── Chevron SVG template ── */
    const CHEVRON_SVG = '<svg class="spider-tree-toggle" width="16" height="16" viewBox="0 0 16 16" fill="none">' +
        '<path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5"/>' +
        '</svg>';

    /* ── Helpers ── */
    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    /* ================================================================
       create(container, options) -- factory
       ================================================================ */
    function create(container, options) {
        options = options || {};

        const idField       = options.idField || 'id';
        const labelField    = options.labelField || 'label';
        const childrenField = options.childrenField || 'children';
        const draggable     = options.draggable === true;
        const onSelect      = options.onSelect || null;
        const onDrop        = options.onDrop || null;
        const renderLabel   = options.renderLabel || null;

        let data       = options.data || [];
        let selectedId = null;
        let nodeMap    = {};  /* id -> { el, itemEl, childrenEl, node, level } */

        /* Drag state */
        let dragNode = null;

        /* Root element */
        const rootEl = document.createElement('div');
        rootEl.className = 'spider-tree';
        Object.assign(rootEl.style, {
            userSelect: 'none',
            fontSize:   'var(--cds-body-01-size)',
            color:      'var(--cds-text-primary)'
        });

        container.innerHTML = '';
        container.appendChild(rootEl);

        /* Drop indicator element (reused) */
        const dropIndicator = document.createElement('div');
        dropIndicator.className = 'spider-tree-drop-indicator';
        Object.assign(dropIndicator.style, {
            position:        'absolute',
            left:            '0',
            right:           '0',
            height:          '2px',
            backgroundColor: 'var(--cds-interactive)',
            pointerEvents:   'none',
            display:         'none',
            zIndex:          '1'
        });

        /* ────────────────────────────────────────────
           Render
           ──────────────────────────────────────────── */
        function render() {
            rootEl.innerHTML = '';
            nodeMap = {};

            if (!data || data.length === 0) {
                const emptyEl = document.createElement('div');
                emptyEl.className = 'spider-tree-empty';
                Object.assign(emptyEl.style, {
                    padding:   '1rem',
                    color:     'var(--cds-text-secondary)',
                    fontSize:  'var(--cds-label-01-size)',
                    textAlign: 'center'
                });
                emptyEl.textContent = t('tree.noChildren');
                rootEl.appendChild(emptyEl);
                return;
            }

            data.forEach(function (node) {
                rootEl.appendChild(renderNode(node, 0));
            });
        }

        function renderNode(node, level) {
            const id       = node[idField];
            const children = node[childrenField] || [];
            const hasChildren = children.length > 0;

            /* Node wrapper */
            const nodeEl = document.createElement('div');
            nodeEl.className = 'spider-tree-node';
            nodeEl.setAttribute('data-node-id', id);

            /* Clickable item row */
            const itemEl = document.createElement('div');
            itemEl.className = 'spider-tree-item';
            Object.assign(itemEl.style, {
                display:       'flex',
                alignItems:    'center',
                paddingLeft:   (level * 24) + 'px',
                paddingRight:  '0.5rem',
                height:        '2rem',
                cursor:        'pointer',
                position:      'relative',
                transition:    'background-color 120ms ease',
                borderRadius:  '0'
            });

            /* Hover style via listeners */
            itemEl.addEventListener('mouseenter', function () {
                if (!itemEl.classList.contains('active')) {
                    itemEl.style.backgroundColor = 'var(--cds-layer-hover-01)';
                }
            });
            itemEl.addEventListener('mouseleave', function () {
                if (!itemEl.classList.contains('active')) {
                    itemEl.style.backgroundColor = '';
                }
            });

            /* Toggle chevron */
            const toggleWrap = document.createElement('span');
            Object.assign(toggleWrap.style, {
                display:        'inline-flex',
                alignItems:     'center',
                justifyContent: 'center',
                width:          '24px',
                height:         '24px',
                flexShrink:     '0'
            });

            if (hasChildren) {
                toggleWrap.innerHTML = CHEVRON_SVG;
                const chevron = toggleWrap.firstChild;
                Object.assign(chevron.style, {
                    transition: 'transform 200ms ease',
                    color:      'var(--cds-icon-secondary)',
                    cursor:     'pointer'
                });
                /* Start expanded */
                chevron.classList.add('expanded');
                chevron.style.transform = 'rotate(90deg)';

                toggleWrap.addEventListener('click', function (e) {
                    e.stopPropagation();
                    toggleNode(id);
                });
            }

            itemEl.appendChild(toggleWrap);

            /* Label */
            const labelEl = document.createElement('span');
            labelEl.className = 'spider-tree-label';
            Object.assign(labelEl.style, {
                flex:         '1',
                overflow:     'hidden',
                whiteSpace:   'nowrap',
                textOverflow: 'ellipsis',
                lineHeight:   '1.43'
            });
            labelEl.textContent = renderLabel ? renderLabel(node) : node[labelField];

            itemEl.appendChild(labelEl);

            /* Click to select */
            itemEl.addEventListener('click', function () {
                selectNode(id);
            });

            nodeEl.appendChild(itemEl);

            /* Children container */
            const childrenEl = document.createElement('div');
            childrenEl.className = 'spider-tree-children';

            if (hasChildren) {
                children.forEach(function (child) {
                    childrenEl.appendChild(renderNode(child, level + 1));
                });
            }

            nodeEl.appendChild(childrenEl);

            /* Store in map */
            nodeMap[id] = { el: nodeEl, itemEl: itemEl, childrenEl: childrenEl, node: node, level: level };

            /* Drag and drop */
            if (draggable) {
                setupDrag(itemEl, nodeEl, node, level);
            }

            return nodeEl;
        }

        /* ────────────────────────────────────────────
           Toggle expand / collapse
           ──────────────────────────────────────────── */
        function toggleNode(id) {
            const entry = nodeMap[id];
            if (!entry) return;

            const chevron = qs('.spider-tree-toggle', entry.itemEl);
            if (!chevron) return;

            const isExpanded = chevron.classList.contains('expanded');
            if (isExpanded) {
                chevron.classList.remove('expanded');
                chevron.style.transform = 'rotate(0deg)';
                entry.childrenEl.style.display = 'none';
            } else {
                chevron.classList.add('expanded');
                chevron.style.transform = 'rotate(90deg)';
                entry.childrenEl.style.display = '';
            }
        }

        function setExpanded(id, expanded) {
            const entry = nodeMap[id];
            if (!entry) return;

            const chevron = qs('.spider-tree-toggle', entry.itemEl);
            if (!chevron) return;

            if (expanded) {
                chevron.classList.add('expanded');
                chevron.style.transform = 'rotate(90deg)';
                entry.childrenEl.style.display = '';
            } else {
                chevron.classList.remove('expanded');
                chevron.style.transform = 'rotate(0deg)';
                entry.childrenEl.style.display = 'none';
            }
        }

        function expandAll() {
            Object.keys(nodeMap).forEach(function (id) {
                setExpanded(id, true);
            });
        }

        function collapseAll() {
            Object.keys(nodeMap).forEach(function (id) {
                setExpanded(id, false);
            });
        }

        /* ────────────────────────────────────────────
           Selection
           ──────────────────────────────────────────── */
        function selectNode(id) {
            /* Deselect previous */
            if (selectedId !== null && nodeMap[selectedId]) {
                const prev = nodeMap[selectedId].itemEl;
                prev.classList.remove('active');
                prev.style.backgroundColor = '';
            }

            selectedId = id;
            const entry = nodeMap[id];
            if (!entry) return;

            entry.itemEl.classList.add('active');
            entry.itemEl.style.backgroundColor = 'var(--cds-background-selected)';

            /* Ensure parent nodes are expanded so the selected node is visible */
            expandParents(id);

            if (typeof onSelect === 'function') {
                onSelect(entry.node);
            }
        }

        function expandParents(id) {
            /* Walk the DOM upward to expand collapsed ancestors */
            const entry = nodeMap[id];
            if (!entry) return;

            let parentNode = entry.el.parentElement;
            while (parentNode && parentNode !== rootEl) {
                if (parentNode.classList.contains('spider-tree-children')) {
                    const parentTreeNode = parentNode.parentElement;
                    if (parentTreeNode) {
                        const parentId = parentTreeNode.getAttribute('data-node-id');
                        if (parentId) setExpanded(parentId, true);
                    }
                }
                parentNode = parentNode.parentElement;
            }
        }

        function getSelected() {
            if (selectedId === null) return null;
            const entry = nodeMap[selectedId];
            return entry ? entry.node : null;
        }

        /* ────────────────────────────────────────────
           Drag and drop
           ──────────────────────────────────────────── */
        function setupDrag(itemEl, nodeEl, node, level) {
            itemEl.setAttribute('draggable', 'true');

            itemEl.addEventListener('dragstart', function (e) {
                dragNode = node;
                e.dataTransfer.effectAllowed = 'move';
                /* Slight delay for visual feedback */
                setTimeout(function () {
                    nodeEl.style.opacity = '0.4';
                }, 0);
            });

            itemEl.addEventListener('dragend', function () {
                nodeEl.style.opacity = '';
                dragNode = null;
                hideDropIndicator();
            });

            itemEl.addEventListener('dragover', function (e) {
                if (!dragNode || dragNode[idField] === node[idField]) return;
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';

                const rect = itemEl.getBoundingClientRect();
                const y    = e.clientY - rect.top;
                const h    = rect.height;

                let position;
                if (y < h * 0.25) {
                    position = 'before';
                } else if (y > h * 0.75) {
                    position = 'after';
                } else {
                    position = 'inside';
                }

                showDropIndicator(itemEl, position, level);
            });

            itemEl.addEventListener('dragleave', function () {
                hideDropIndicator();
                itemEl.style.backgroundColor = '';
            });

            itemEl.addEventListener('drop', function (e) {
                e.preventDefault();
                if (!dragNode || dragNode[idField] === node[idField]) return;

                const rect = itemEl.getBoundingClientRect();
                const y    = e.clientY - rect.top;
                const h    = rect.height;

                let position;
                if (y < h * 0.25) {
                    position = 'before';
                } else if (y > h * 0.75) {
                    position = 'after';
                } else {
                    position = 'inside';
                }

                hideDropIndicator();
                itemEl.style.backgroundColor = '';

                if (typeof onDrop === 'function') {
                    onDrop(dragNode, node, position);
                }
            });
        }

        function showDropIndicator(targetItemEl, position, level) {
            /* Remove any existing 'inside' highlight */
            qsa('.spider-tree-item', rootEl).forEach(function (el) {
                if (!el.classList.contains('active')) {
                    el.style.backgroundColor = '';
                }
            });

            if (position === 'inside') {
                targetItemEl.style.backgroundColor = 'var(--cds-background-selected-hover)';
                dropIndicator.style.display = 'none';
            } else {
                /* Show line indicator */
                if (!dropIndicator.parentNode) {
                    document.body.appendChild(dropIndicator);
                }

                const rect   = targetItemEl.getBoundingClientRect();
                const indent = level * 24;

                dropIndicator.style.display  = 'block';
                dropIndicator.style.position = 'fixed';
                dropIndicator.style.left     = (rect.left + indent) + 'px';
                dropIndicator.style.width    = (rect.width - indent) + 'px';

                if (position === 'before') {
                    dropIndicator.style.top = rect.top + 'px';
                } else {
                    dropIndicator.style.top = (rect.bottom - 2) + 'px';
                }
            }
        }

        function hideDropIndicator() {
            dropIndicator.style.display = 'none';
        }

        /* ────────────────────────────────────────────
           Data management
           ──────────────────────────────────────────── */
        function setData(newData) {
            data = newData || [];
            selectedId = null;
            render();
        }

        /* Find node recursively by id */
        function findNode(nodes, id) {
            for (let i = 0; i < nodes.length; i++) {
                if (nodes[i][idField] === id) return nodes[i];
                const children = nodes[i][childrenField] || [];
                if (children.length > 0) {
                    const found = findNode(children, id);
                    if (found) return found;
                }
            }
            return null;
        }

        /* ── Initial render ── */
        render();

        /* ── Return public interface ── */
        return {
            setData:     setData,
            getSelected: getSelected,
            expandAll:   expandAll,
            collapseAll: collapseAll,
            selectNode:  selectNode,
            findNode:    function (id) { return findNode(data, id); },
            /** Direct access for advanced usage */
            el:          rootEl
        };
    }

    /* ================================================================
       Export
       ================================================================ */
    window.SpiderTree = {
        create: create
    };
})();
