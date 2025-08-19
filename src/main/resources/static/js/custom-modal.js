/**
 * 自定义弹窗组件
 * 替换浏览器原生的alert、confirm等弹窗
 */

class CustomModal {
    constructor() {
        this.init();
    }

    init() {
        // 创建弹窗容器
        this.createModalContainer();
        
        // 绑定事件
        this.bindEvents();
    }

    createModalContainer() {
        // 创建弹窗容器
        const modalContainer = document.createElement('div');
        modalContainer.id = 'custom-modal-container';
        modalContainer.innerHTML = `
            <div id="custom-modal-overlay" class="custom-modal-overlay"></div>
            <div id="custom-modal" class="custom-modal">
                <div class="custom-modal-header">
                    <h3 id="custom-modal-title" class="custom-modal-title"></h3>
                    <button id="custom-modal-close" class="custom-modal-close">&times;</button>
                </div>
                <div id="custom-modal-content" class="custom-modal-content"></div>
                <div id="custom-modal-footer" class="custom-modal-footer"></div>
            </div>
        `;
        
        document.body.appendChild(modalContainer);
        
        // 添加样式
        this.addStyles();
    }

    addStyles() {
        if (document.getElementById('custom-modal-styles')) {
            return; // 样式已存在
        }

        const style = document.createElement('style');
        style.id = 'custom-modal-styles';
        style.textContent = `
            .custom-modal-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                z-index: 9998;
                opacity: 0;
                visibility: hidden;
                transition: all 0.3s ease;
            }

            .custom-modal-overlay.show {
                opacity: 1;
                visibility: visible;
            }

            .custom-modal {
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%) scale(0.7);
                background: white;
                border-radius: 12px;
                box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                z-index: 9999;
                min-width: 400px;
                max-width: 90vw;
                max-height: 90vh;
                opacity: 0;
                visibility: hidden;
                transition: all 0.3s ease;
            }

            .custom-modal.show {
                opacity: 1;
                visibility: visible;
                transform: translate(-50%, -50%) scale(1);
            }

            .custom-modal-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 20px 24px 0;
                border-bottom: 1px solid #e5e7eb;
                padding-bottom: 16px;
            }

            .custom-modal-title {
                margin: 0;
                font-size: 18px;
                font-weight: 600;
                color: #111827;
            }

            .custom-modal-close {
                background: none;
                border: none;
                font-size: 24px;
                color: #9ca3af;
                cursor: pointer;
                padding: 0;
                width: 32px;
                height: 32px;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 6px;
                transition: all 0.2s ease;
            }

            .custom-modal-close:hover {
                background: #f3f4f6;
                color: #6b7280;
            }

            .custom-modal-content {
                padding: 20px 24px;
                color: #374151;
                line-height: 1.6;
                max-height: 60vh;
                overflow-y: auto;
            }

            .custom-modal-footer {
                padding: 16px 24px 20px;
                display: flex;
                justify-content: flex-end;
                gap: 12px;
                border-top: 1px solid #e5e7eb;
            }

            .custom-modal-btn {
                padding: 10px 20px;
                border: none;
                border-radius: 8px;
                font-size: 14px;
                font-weight: 500;
                cursor: pointer;
                transition: all 0.2s ease;
                min-width: 80px;
            }

            .custom-modal-btn-primary {
                background: #3b82f6;
                color: white;
            }

            .custom-modal-btn-primary:hover {
                background: #2563eb;
            }

            .custom-modal-btn-secondary {
                background: #f3f4f6;
                color: #374151;
                border: 1px solid #d1d5db;
            }

            .custom-modal-btn-secondary:hover {
                background: #e5e7eb;
            }

            .custom-modal-btn-danger {
                background: #ef4444;
                color: white;
            }

            .custom-modal-btn-danger:hover {
                background: #dc2626;
            }

            .custom-modal-btn-success {
                background: #10b981;
                color: white;
            }

            .custom-modal-btn-success:hover {
                background: #059669;
            }

            .custom-modal-btn-warning {
                background: #f59e0b;
                color: white;
            }

            .custom-modal-btn-warning:hover {
                background: #d97706;
            }

            .custom-modal-icon {
                display: inline-block;
                width: 24px;
                height: 24px;
                margin-right: 8px;
                vertical-align: middle;
            }

            .custom-modal-success .custom-modal-title {
                color: #059669;
            }

            .custom-modal-error .custom-modal-title {
                color: #dc2626;
            }

            .custom-modal-warning .custom-modal-title {
                color: #d97706;
            }

            .custom-modal-info .custom-modal-title {
                color: #2563eb;
            }

            .custom-modal-loading {
                text-align: center;
                padding: 40px 20px;
            }

            .custom-modal-loading .spinner {
                display: inline-block;
                width: 40px;
                height: 40px;
                border: 4px solid #f3f4f6;
                border-top: 4px solid #3b82f6;
                border-radius: 50%;
                animation: spin 1s linear infinite;
                margin-bottom: 16px;
            }

            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }

            .custom-modal-table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 16px;
            }

            .custom-modal-table th,
            .custom-modal-table td {
                padding: 12px;
                text-align: left;
                border-bottom: 1px solid #e5e7eb;
            }

            .custom-modal-table th {
                background: #f9fafb;
                font-weight: 600;
                color: #374151;
            }

            .custom-modal-table tr:hover {
                background: #f9fafb;
            }

            .custom-modal-form {
                display: flex;
                flex-direction: column;
                gap: 16px;
            }

            .custom-modal-form-group {
                display: flex;
                flex-direction: column;
                gap: 6px;
            }

            .custom-modal-form-label {
                font-weight: 500;
                color: #374151;
                font-size: 14px;
            }

            .custom-modal-form-input {
                padding: 10px 12px;
                border: 1px solid #d1d5db;
                border-radius: 6px;
                font-size: 14px;
                transition: border-color 0.2s ease;
            }

            .custom-modal-form-input:focus {
                outline: none;
                border-color: #3b82f6;
                box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
            }

            .custom-modal-form-textarea {
                min-height: 100px;
                resize: vertical;
            }

            .custom-modal-form-select {
                background: white;
            }

            .custom-modal-badge {
                display: inline-block;
                padding: 4px 8px;
                border-radius: 12px;
                font-size: 12px;
                font-weight: 500;
                text-transform: uppercase;
            }

            .custom-modal-badge-success {
                background: #d1fae5;
                color: #065f46;
            }

            .custom-modal-badge-error {
                background: #fee2e2;
                color: #991b1b;
            }

            .custom-modal-badge-warning {
                background: #fef3c7;
                color: #92400e;
            }

            .custom-modal-badge-info {
                background: #dbeafe;
                color: #1e40af;
            }
        `;
        
        document.head.appendChild(style);
    }

    bindEvents() {
        // 关闭弹窗事件
        document.getElementById('custom-modal-close').addEventListener('click', () => {
            this.hide();
        });

        // 点击遮罩层关闭弹窗
        document.getElementById('custom-modal-overlay').addEventListener('click', () => {
            this.hide();
        });

        // ESC键关闭弹窗
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isVisible()) {
                this.hide();
            }
        });
    }

    show(options = {}) {
        const {
            title = '提示',
            content = '',
            type = 'info', // success, error, warning, info
            buttons = [],
            width = 'auto',
            height = 'auto',
            closable = true,
            onClose = null
        } = options;

        const modal = document.getElementById('custom-modal');
        const overlay = document.getElementById('custom-modal-overlay');
        const titleEl = document.getElementById('custom-modal-title');
        const contentEl = document.getElementById('custom-modal-content');
        const footerEl = document.getElementById('custom-modal-footer');
        const closeBtn = document.getElementById('custom-modal-close');

        // 设置标题
        titleEl.textContent = title;

        // 设置内容
        if (typeof content === 'string') {
            contentEl.innerHTML = content;
        } else if (content instanceof HTMLElement) {
            contentEl.innerHTML = '';
            contentEl.appendChild(content);
        }

        // 设置类型样式
        modal.className = `custom-modal custom-modal-${type}`;

        // 设置尺寸
        if (width !== 'auto') {
            modal.style.width = width;
        }
        if (height !== 'auto') {
            modal.style.height = height;
        }

        // 设置按钮
        this.setButtons(footerEl, buttons);

        // 设置关闭按钮
        closeBtn.style.display = closable ? 'block' : 'none';

        // 显示弹窗
        overlay.classList.add('show');
        modal.classList.add('show');

        // 保存关闭回调
        this.onCloseCallback = onClose;
    }

    setButtons(footerEl, buttons) {
        footerEl.innerHTML = '';

        if (buttons.length === 0) {
            // 默认按钮
            const defaultBtn = this.createButton('确定', 'primary', () => this.hide());
            footerEl.appendChild(defaultBtn);
        } else {
            buttons.forEach(button => {
                const btn = this.createButton(
                    button.text,
                    button.type || 'secondary',
                    button.onClick || (() => this.hide())
                );
                footerEl.appendChild(btn);
            });
        }
    }

    createButton(text, type, onClick) {
        const button = document.createElement('button');
        button.className = `custom-modal-btn custom-modal-btn-${type}`;
        button.textContent = text;
        button.addEventListener('click', onClick);
        return button;
    }

    hide() {
        const modal = document.getElementById('custom-modal');
        const overlay = document.getElementById('custom-modal-overlay');

        overlay.classList.remove('show');
        modal.classList.remove('show');

        // 执行关闭回调
        if (this.onCloseCallback) {
            this.onCloseCallback();
            this.onCloseCallback = null;
        }
    }

    isVisible() {
        const modal = document.getElementById('custom-modal');
        return modal.classList.contains('show');
    }

    // 便捷方法
    alert(message, title = '提示', type = 'info') {
        this.show({
            title,
            content: message,
            type,
            buttons: [
                { text: '确定', type: 'primary' }
            ]
        });
    }

    confirm(message, title = '确认', onConfirm, onCancel) {
        this.show({
            title,
            content: message,
            type: 'warning',
            buttons: [
                { text: '取消', type: 'secondary', onClick: () => { this.hide(); if (onCancel) onCancel(); } },
                { text: '确定', type: 'primary', onClick: () => { this.hide(); if (onConfirm) onConfirm(); } }
            ]
        });
    }

    prompt(message, title = '输入', defaultValue = '', onConfirm, onCancel) {
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'custom-modal-form-input';
        input.value = defaultValue;
        input.placeholder = '请输入...';

        const content = document.createElement('div');
        content.innerHTML = `<p>${message}</p>`;
        content.appendChild(input);

        this.show({
            title,
            content,
            type: 'info',
            buttons: [
                { text: '取消', type: 'secondary', onClick: () => { this.hide(); if (onCancel) onCancel(); } },
                { text: '确定', type: 'primary', onClick: () => { this.hide(); if (onConfirm) onConfirm(input.value); } }
            ]
        });

        // 聚焦输入框
        setTimeout(() => input.focus(), 100);
    }

    success(message, title = '成功') {
        this.alert(message, title, 'success');
    }

    error(message, title = '错误') {
        this.alert(message, title, 'error');
    }

    warning(message, title = '警告') {
        this.alert(message, title, 'warning');
    }

    info(message, title = '信息') {
        this.alert(message, title, 'info');
    }

    loading(message = '加载中...', title = '请稍候') {
        const content = `
            <div class="custom-modal-loading">
                <div class="spinner"></div>
                <p>${message}</p>
            </div>
        `;

        this.show({
            title,
            content,
            type: 'info',
            closable: false,
            buttons: []
        });
    }

    table(data, columns, title = '数据表格') {
        const table = document.createElement('table');
        table.className = 'custom-modal-table';

        // 创建表头
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        columns.forEach(column => {
            const th = document.createElement('th');
            th.textContent = column.title;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // 创建表体
        const tbody = document.createElement('tbody');
        data.forEach(row => {
            const tr = document.createElement('tr');
            columns.forEach(column => {
                const td = document.createElement('td');
                const value = row[column.key];
                
                if (column.render) {
                    td.innerHTML = column.render(value, row);
                } else if (column.type === 'badge') {
                    const badge = document.createElement('span');
                    badge.className = `custom-modal-badge custom-modal-badge-${value}`;
                    badge.textContent = value;
                    td.appendChild(badge);
                } else {
                    td.textContent = value || '-';
                }
                
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);

        this.show({
            title,
            content: table,
            type: 'info',
            width: '800px',
            buttons: [
                { text: '关闭', type: 'primary' }
            ]
        });
    }
}

// 创建全局实例
window.customModal = new CustomModal();

// 替换原生方法
window.alert = (message) => window.customModal.alert(message);
window.confirm = (message) => {
    return new Promise((resolve) => {
        window.customModal.confirm(message, '确认', () => resolve(true), () => resolve(false));
    });
};
