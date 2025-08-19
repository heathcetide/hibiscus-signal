// API 文档管理类
class ApiDocsManager {
    constructor() {
        this.apiData = [];
        this.filteredData = [];
        this.groupedData = {};
        this.currentPage = 1;
        this.itemsPerPage = 10;
        this.init();
    }

    async init() {
        await this.loadApiData();
        this.setupEventListeners();
        this.setupTabSwitching();
        this.groupApisByController();
        this.renderApiList();
        this.updateStats();
        this.updatePagination();
    }

    // 设置标签页切换
    setupTabSwitching() {
        const docsTab = document.getElementById('docsTab');
        const testTab = document.getElementById('testTab');
        const monitorTab = document.getElementById('monitorTab');
        const docsContent = document.getElementById('docsContent');
        const testContent = document.getElementById('testContent');
        const monitorContent = document.getElementById('monitorContent');

        docsTab.addEventListener('click', () => {
            this.switchTab('docs');
        });

        testTab.addEventListener('click', () => {
            this.switchTab('test');
        });

        monitorTab.addEventListener('click', () => {
            this.switchTab('monitor');
        });

        // 监听 HTTP 方法变化，显示/隐藏请求体
        document.getElementById('testMethod').addEventListener('change', (e) => {
            const method = e.target.value;
            const requestBodySection = document.getElementById('requestBodySection');
            if (method === 'POST' || method === 'PUT' || method === 'PATCH') {
                requestBodySection.classList.remove('hidden');
            } else {
                requestBodySection.classList.add('hidden');
            }
        });
    }

    // 切换标签页
    switchTab(tabName) {
        const docsTab = document.getElementById('docsTab');
        const testTab = document.getElementById('testTab');
        const monitorTab = document.getElementById('monitorTab');
        const docsContent = document.getElementById('docsContent');
        const testContent = document.getElementById('testContent');
        const monitorContent = document.getElementById('monitorContent');

        // 重置所有标签页状态
        docsTab.classList.remove('bg-white', 'bg-opacity-20');
        testTab.classList.remove('bg-white', 'bg-opacity-20');
        monitorTab.classList.remove('bg-white', 'bg-opacity-20');
        docsContent.classList.add('hidden');
        testContent.classList.add('hidden');
        monitorContent.classList.add('hidden');

        // 激活选中的标签页
        if (tabName === 'docs') {
            docsTab.classList.add('bg-white', 'bg-opacity-20');
            docsContent.classList.remove('hidden');
        } else if (tabName === 'test') {
            testTab.classList.add('bg-white', 'bg-opacity-20');
            testContent.classList.remove('hidden');
        } else if (tabName === 'monitor') {
            monitorTab.classList.add('bg-white', 'bg-opacity-20');
            monitorContent.classList.remove('hidden');
            // 加载监控数据
            refreshMonitorData();
        }
    }

    // 加载 API 数据 - 从内存中获取
    async loadApiData() {
        try {
            // 从内存中的 MappingHandler 获取数据
            const response = await fetch('/test/api/docs');
            if (response.ok) {
                this.apiData = await response.json();
                this.filteredData = [...this.apiData];
                this.hideLoading();
            } else {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
        } catch (error) {
            console.error('加载 API 数据失败:', error);
            this.showError('加载 API 数据失败，请检查后端服务');
            this.hideLoading();
        }
    }

    // 按控制器分组 API
    groupApisByController() {
        this.groupedData = {};
        this.filteredData.forEach(api => {
            const controllerName = api.className;
            if (!this.groupedData[controllerName]) {
                this.groupedData[controllerName] = [];
            }
            this.groupedData[controllerName].push(api);
        });
    }

    // 设置事件监听器
    setupEventListeners() {
        // 搜索框输入事件
        document.getElementById('searchInput').addEventListener('input', (e) => {
            this.filterApis();
        });

        // 方法筛选事件
        document.getElementById('methodFilter').addEventListener('change', (e) => {
            this.filterApis();
        });

        // 分页事件
        document.getElementById('prevPage').addEventListener('click', () => {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.renderApiList();
                this.updatePagination();
            }
        });

        document.getElementById('nextPage').addEventListener('click', () => {
            const maxPage = Math.ceil(this.getTotalFilteredApis() / this.itemsPerPage);
            if (this.currentPage < maxPage) {
                this.currentPage++;
                this.renderApiList();
                this.updatePagination();
            }
        });

        // 每页显示数量变化
        document.getElementById('itemsPerPage').addEventListener('change', (e) => {
            this.itemsPerPage = parseInt(e.target.value);
            this.currentPage = 1;
            this.renderApiList();
            this.updatePagination();
        });
    }

    // 筛选 API
    filterApis() {
        const searchTerm = document.getElementById('searchInput').value.toLowerCase();
        const methodFilter = document.getElementById('methodFilter').value;

        this.filteredData = this.apiData.filter(api => {
            const matchesSearch = 
                api.methodName.toLowerCase().includes(searchTerm) ||
                api.className.toLowerCase().includes(searchTerm) ||
                api.paths.some(path => path.toLowerCase().includes(searchTerm));

            const matchesMethod = !methodFilter || api.methodType === methodFilter;

            return matchesSearch && matchesMethod;
        });

        this.currentPage = 1;
        this.groupApisByController();
        this.renderApiList();
        this.updateStats();
        this.updatePagination();
    }

    // 获取筛选后的 API 总数
    getTotalFilteredApis() {
        return this.filteredData.length;
    }

    // 渲染 API 列表
    renderApiList() {
        const container = document.getElementById('apiList');
        
        if (this.filteredData.length === 0) {
            container.innerHTML = `
                <div class="text-center py-16">
                    <div class="mx-auto flex items-center justify-center h-20 w-20 rounded-full bg-gray-100 mb-6">
                        <i class="fas fa-search text-gray-400 text-3xl"></i>
                    </div>
                    <h3 class="text-xl font-medium text-gray-900 mb-3">未找到匹配的 API</h3>
                    <p class="text-gray-500 text-lg">请尝试调整搜索条件或筛选条件</p>
                </div>
            `;
            return;
        }

        // 计算分页
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const paginatedGroups = Object.entries(this.groupedData).slice(startIndex, endIndex);

        let html = '';
        paginatedGroups.forEach(([controllerName, apis]) => {
            html += this.createControllerGroup(controllerName, apis);
        });

        container.innerHTML = html;

        // 重新绑定折叠事件
        this.bindCollapseEvents();
    }

    // 创建控制器分组
    createControllerGroup(controllerName, apis) {
        const totalApis = apis.length;
        const methodCounts = this.getMethodCounts(apis);
        
        return `
            <div class="bg-white rounded-xl shadow-lg hover:shadow-xl transition-all duration-300 border-l-4 border-blue-500 mb-4 overflow-hidden" 
                 data-controller="${controllerName}">
                <div class="p-5">
                    <!-- 控制器头部 -->
                    <div class="flex items-center justify-between mb-4 cursor-pointer group" 
                         onclick="window.apiDocsManager.toggleController('${controllerName}')">
                        <div class="flex items-center space-x-4">
                            <div class="flex items-center justify-center w-10 h-10 bg-gradient-to-br from-blue-100 to-blue-200 rounded-lg group-hover:from-blue-200 group-hover:to-blue-300 transition-all duration-200">
                                <i class="fas fa-cube text-blue-600 text-lg"></i>
                            </div>
                            <div>
                                <h3 class="text-lg font-bold text-gray-900 group-hover:text-blue-600 transition-colors duration-200">${controllerName}</h3>
                                <div class="flex items-center space-x-2 mt-1">
                                    <span class="text-sm text-gray-600">${totalApis} 个接口</span>
                                    <span class="text-gray-400">•</span>
                                    <div class="flex space-x-1">
                                        ${Object.entries(methodCounts).map(([method, count]) => 
                                            `<span class="px-2 py-1 bg-gray-100 text-gray-700 text-xs rounded-full font-medium">${method}: ${count}</span>`
                                        ).join('')}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="flex items-center space-x-3">
                            <span class="text-sm text-gray-500 opacity-0 group-hover:opacity-100 transition-opacity duration-200">点击展开/折叠</span>
                            <i class="fas fa-chevron-down text-gray-400 transition-all duration-300 transform" 
                               id="chevron-${controllerName}"></i>
                        </div>
                    </div>
                    
                    <!-- API 列表 -->
                    <div class="space-y-3 transition-all duration-300 ease-in-out" id="apis-${controllerName}">
                        ${apis.map((api, index) => this.createApiCard(api, index)).join('')}
                    </div>
                </div>
            </div>
        `;
    }

    // 获取方法统计
    getMethodCounts(apis) {
        const counts = {};
        apis.forEach(api => {
            counts[api.methodType] = (counts[api.methodType] || 0) + 1;
        });
        return counts;
    }

    // 切换控制器展开/折叠
    toggleController(controllerName) {
        const apisContainer = document.getElementById(`apis-${controllerName}`);
        const chevron = document.getElementById(`chevron-${controllerName}`);
        const controllerCard = document.querySelector(`[data-controller="${controllerName}"]`);
        
        if (apisContainer.classList.contains('collapsed')) {
            // 展开
            apisContainer.classList.remove('collapsed');
            apisContainer.style.maxHeight = apisContainer.scrollHeight + 'px';
            apisContainer.style.opacity = '1';
            chevron.classList.remove('rotate-180');
            controllerCard.classList.add('expanded');
        } else {
            // 折叠
            apisContainer.classList.add('collapsed');
            apisContainer.style.maxHeight = '0';
            apisContainer.style.opacity = '0';
            chevron.classList.add('rotate-180');
            controllerCard.classList.remove('expanded');
        }
    }

    // 绑定折叠事件
    bindCollapseEvents() {
        // 事件已经在 HTML 中绑定，这里可以添加其他逻辑
    }

    // 创建 API 卡片
    createApiCard(api, index) {
        const methodClass = `method-${api.methodType.toLowerCase()}`;
        const parametersHtml = this.renderParameters(api.parameters);
        
        return `
            <div class="bg-gradient-to-r from-gray-50 to-gray-100 rounded-lg p-4 hover:from-gray-100 hover:to-gray-200 transition-all duration-200 border border-gray-200 hover:border-blue-300 hover:shadow-md transform hover:-translate-y-1">
                <div class="flex items-center justify-between mb-3">
                    <div class="flex items-center space-x-3">
                        <span class="px-3 py-1 rounded-full text-white text-sm font-bold shadow-sm ${methodClass}">${api.methodType}</span>
                        <h4 class="text-base font-semibold text-gray-900">${api.methodName}</h4>
                    </div>
                    <button onclick="testApi('${api.methodType}', '${api.paths[0]}')"
                            class="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-all duration-200 flex items-center transform hover:scale-105 active:scale-95">
                        <i class="fas fa-play mr-1.5"></i>测试
                    </button>
                </div>
                
                <div class="mb-3">
                    <p class="text-sm font-medium text-gray-700 mb-2 flex items-center">
                        <i class="fas fa-link text-blue-500 mr-2 text-xs"></i>API 路径
                    </p>
                    <div class="flex flex-wrap gap-2">
                        ${api.paths.map(path => `
                            <code class="px-2.5 py-1 bg-blue-50 text-blue-700 rounded-md text-xs font-mono border border-blue-200">${path}</code>
                        `).join('')}
                    </div>
                </div>
                
                ${parametersHtml ? `
                    <div class="mb-3">
                        <p class="text-sm font-medium text-gray-700 mb-2 flex items-center">
                            <i class="fas fa-cogs text-purple-500 mr-2 text-xs"></i>参数信息
                        </p>
                        ${parametersHtml}
                    </div>
                ` : ''}
            </div>
        `;
    }

    // 渲染参数信息
    renderParameters(parameters) {
        if (!parameters || Object.keys(parameters).length === 0) {
            return '<p class="text-gray-500 text-sm italic">无参数</p>';
        }

        const paramItems = [];
        
        // 处理普通参数
        Object.keys(parameters).forEach(key => {
            if (!key.includes('_') && key !== 'body' && key !== 'bodyFields') {
                const value = parameters[key];
                const required = parameters[key + '_required'];
                const defaultValue = parameters[key + '_defaultValue'];
                
                let paramHtml = `
                    <div class="bg-white rounded-lg p-3 mb-2 border border-gray-200 hover:border-blue-300 transition-colors duration-200">
                        <div class="flex items-center justify-between mb-2">
                            <span class="font-medium text-gray-900 text-sm">${key}</span>
                            ${required ? 
                                '<span class="px-2 py-1 bg-red-100 text-red-800 text-xs font-medium rounded-full">必需</span>' : 
                                '<span class="px-2 py-1 bg-gray-100 text-gray-800 text-xs font-medium rounded-full">可选</span>'
                            }
                        </div>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs">
                            <div class="text-gray-600">类型: <span class="font-mono bg-gray-100 px-1.5 py-0.5 rounded">${value}</span></div>
                            ${defaultValue ? `<div class="text-gray-600">默认值: <span class="font-mono bg-gray-100 px-1.5 py-0.5 rounded">${defaultValue}</span></div>` : ''}
                        </div>
                    </div>
                `;
                paramItems.push(paramHtml);
            }
        });

        // 处理请求体
        if (parameters.body) {
            let bodyHtml = `
                <div class="bg-white rounded-lg p-3 mb-2 border border-gray-200 hover:border-blue-300 transition-colors duration-200">
                    <div class="flex items-center justify-between mb-2">
                        <span class="font-medium text-gray-900 text-sm">请求体</span>
                        <span class="text-gray-600 text-xs bg-blue-100 px-2 py-1 rounded-full">${parameters.body}</span>
                    </div>
            `;
            
            if (parameters.bodyFields && Array.isArray(parameters.bodyFields)) {
                bodyHtml += `
                    <div class="mt-3">
                        <p class="text-xs text-gray-600 mb-2">字段:</p>
                        <div class="flex flex-wrap gap-1.5">
                            ${parameters.bodyFields.map(field => 
                                `<span class="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full border border-blue-200">${field}</span>`
                            ).join('')}
                        </div>
                    </div>
                `;
            }
            
            bodyHtml += '</div>';
            paramItems.push(bodyHtml);
        }

        return paramItems.join('');
    }

    // 更新统计信息
    updateStats() {
        document.getElementById('totalApis').textContent = this.filteredData.length;
        
        const controllers = new Set(this.filteredData.map(api => api.className));
        document.getElementById('totalControllers').textContent = controllers.size;
        
        const methods = new Set(this.filteredData.map(api => api.methodName));
        document.getElementById('totalMethods').textContent = methods.size;
        
        document.getElementById('lastUpdate').textContent = new Date().toLocaleTimeString();
    }

    // 更新分页信息
    updatePagination() {
        const totalApis = this.getTotalFilteredApis();
        const maxPage = Math.ceil(totalApis / this.itemsPerPage);
        
        document.getElementById('currentPage').textContent = this.currentPage;
        document.getElementById('totalPages').textContent = maxPage;
        document.getElementById('totalItems').textContent = totalApis;
        
        // 更新分页按钮状态
        document.getElementById('prevPage').disabled = this.currentPage <= 1;
        document.getElementById('nextPage').disabled = this.currentPage >= maxPage;
        
        // 更新分页按钮样式
        if (this.currentPage <= 1) {
            document.getElementById('prevPage').classList.add('opacity-50', 'cursor-not-allowed');
        } else {
            document.getElementById('prevPage').classList.remove('opacity-50', 'cursor-not-allowed');
        }
        
        if (this.currentPage >= maxPage) {
            document.getElementById('nextPage').classList.add('opacity-50', 'cursor-not-allowed');
        } else {
            document.getElementById('nextPage').classList.remove('opacity-50', 'cursor-not-allowed');
        }
    }

    // 隐藏加载提示
    hideLoading() {
        document.getElementById('loading').style.display = 'none';
    }

    // 显示错误信息
    showError(message) {
        document.getElementById('apiList').innerHTML = `
            <div class="bg-red-50 border border-red-200 rounded-lg p-6">
                <div class="flex items-center">
                    <i class="fas fa-exclamation-triangle text-red-400 text-xl mr-3"></i>
                    <div>
                        <h3 class="text-red-800 font-medium">加载失败</h3>
                        <p class="text-red-700 text-sm">${message}</p>
                    </div>
                </div>
            </div>
        `;
    }

    // 刷新数据
    async refreshData() {
        document.getElementById('loading').style.display = 'block';
        document.getElementById('apiList').innerHTML = '';
        await this.loadApiData();
        this.groupApisByController();
        this.renderApiList();
        this.updateStats();
        this.updatePagination();
    }
}

// 接口测试功能
async function sendTestRequest() {
    const method = document.getElementById('testMethod').value;
    const url = document.getElementById('testUrl').value;
    const headersText = document.getElementById('testHeaders').value;
    const body = document.getElementById('testBody').value;
    const environment = document.getElementById('testEnvironment').value;
    const accessToken = document.getElementById('accessToken').value;
    const timeoutSeconds = document.getElementById('timeoutSeconds').value;
    const globalHeadersText = document.getElementById('globalHeaders').value;

    if (!url) {
        alert('请输入请求 URL');
        return;
    }

    // 构建完整URL
    const baseUrl = getEnvironmentBaseUrl(environment);
    const fullUrl = url.startsWith('http') ? url : baseUrl + url;

    // 重置响应区域
    document.getElementById('responseStatus').textContent = '发送中...';
    document.getElementById('responseHeaders').textContent = '发送中...';
    document.getElementById('responseBody').textContent = '发送中...';
    document.getElementById('responseTime').textContent = '发送中...';

    const startTime = Date.now();

    try {
        let requestOptions = {
            method: method,
            headers: {},
            mode: 'cors',
            credentials: 'omit'
        };

        // 合并全局请求头
        if (globalHeadersText.trim()) {
            try {
                const globalHeaders = JSON.parse(globalHeadersText);
                Object.assign(requestOptions.headers, globalHeaders);
            } catch (e) {
                console.warn('全局请求头格式错误');
            }
        }

        // 添加访问令牌
        if (accessToken.trim()) {
            requestOptions.headers['Authorization'] = `Bearer ${accessToken}`;
        }

        // 解析请求头
        if (headersText.trim()) {
            try {
                const headers = JSON.parse(headersText);
                Object.assign(requestOptions.headers, headers);
            } catch (e) {
                console.warn('请求头格式错误，使用默认请求头');
            }
        }

        // 添加默认请求头
        if (!requestOptions.headers['Content-Type']) {
            requestOptions.headers['Content-Type'] = 'application/json';
        }

        // 添加请求体
        if ((method === 'POST' || method === 'PUT' || method === 'PATCH') && body.trim()) {
            try {
                requestOptions.body = JSON.parse(body);
            } catch (e) {
                requestOptions.body = body;
            }
        }

        console.log('发送请求:', {
            url: fullUrl,
            method: method,
            headers: requestOptions.headers,
            body: requestOptions.body,
            environment: environment,
            timeout: timeoutSeconds
        });

        // 创建 AbortController 用于超时控制
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeoutSeconds * 1000);
        
        requestOptions.signal = controller.signal;

        const response = await fetch(fullUrl, requestOptions);
        clearTimeout(timeoutId);
        
        const endTime = Date.now();
        const responseTime = endTime - startTime;

        console.log('收到响应:', response);

        // 更新响应状态
        document.getElementById('responseStatus').textContent = `${response.status} ${response.statusText}`;
        document.getElementById('responseStatus').className = `px-3 py-2 rounded-lg text-sm font-mono ${
            response.ok ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
        }`;

        // 更新响应头
        const headers = {};
        response.headers.forEach((value, key) => {
            headers[key] = value;
        });
        document.getElementById('responseHeaders').textContent = JSON.stringify(headers, null, 2);

        // 更新响应体
        try {
            const responseText = await response.text();
            let responseBody;
            try {
                responseBody = JSON.parse(responseText);
                document.getElementById('responseBody').textContent = JSON.stringify(responseBody, null, 2);
            } catch {
                document.getElementById('responseBody').textContent = responseText;
            }
        } catch (e) {
            document.getElementById('responseBody').textContent = '无法读取响应体';
        }

        // 更新响应时间
        document.getElementById('responseTime').textContent = `${responseTime}ms`;

        // 添加到请求历史
        addToRequestHistory({
            method: method,
            url: fullUrl,
            status: response.status,
            time: responseTime,
            timestamp: new Date().toLocaleTimeString()
        });

    } catch (error) {
        const endTime = Date.now();
        const responseTime = endTime - startTime;

        console.error('请求失败:', error);

        let errorMessage = error.message;
        if (error.name === 'AbortError') {
            errorMessage = `请求超时 (${timeoutSeconds}秒)`;
        } else if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
            errorMessage = '网络请求失败，可能是 CORS 问题或服务器不可达';
        }

        document.getElementById('responseStatus').textContent = '请求失败';
        document.getElementById('responseStatus').className = 'px-3 py-2 bg-red-100 text-red-800 rounded-lg text-sm font-mono';
        document.getElementById('responseHeaders').textContent = '-';
        document.getElementById('responseBody').textContent = errorMessage;
        document.getElementById('responseTime').textContent = `${responseTime}ms`;

        // 添加到请求历史（失败）
        addToRequestHistory({
            method: method,
            url: fullUrl,
            status: 'FAILED',
            time: responseTime,
            timestamp: new Date().toLocaleTimeString(),
            error: errorMessage
        });
    }
}

// 获取环境基础URL
function getEnvironmentBaseUrl(environment) {
    // 从后端配置获取环境信息
    if (window.environmentConfigs && window.environmentConfigs[environment]) {
        return window.environmentConfigs[environment].baseUrl;
    }
    
    // 默认环境配置
    const defaultEnvironments = {
        'local': 'http://localhost:8080',
        'dev': 'https://dev-api.example.com',
        'prod': 'https://api.example.com'
    };
    return defaultEnvironments[environment] || defaultEnvironments['local'];
}

// 加载环境配置
async function loadEnvironmentConfigs() {
    try {
        const response = await fetch('/test/api/environments');
        if (response.ok) {
            const config = await response.json();
            window.environmentConfigs = config.environments;
            window.defaultHeaders = config.defaultHeaders;
            window.timeoutConfig = config.timeout;
            
            // 更新前端配置
            updateEnvironmentSelect();
            updateDefaultHeaders();
            updateTimeoutConfig();
        }
    } catch (error) {
        console.warn('无法加载环境配置，使用默认配置:', error);
    }
}

// 更新环境选择器
function updateEnvironmentSelect() {
    const select = document.getElementById('testEnvironment');
    if (!select || !window.environmentConfigs) return;
    
    // 清空现有选项
    select.innerHTML = '';
    
    // 添加环境选项
    Object.entries(window.environmentConfigs).forEach(([key, env]) => {
        const option = document.createElement('option');
        option.value = key;
        option.textContent = `${env.description || key} (${env.baseUrl})`;
        select.appendChild(option);
    });
}

// 更新默认请求头
function updateDefaultHeaders() {
    if (window.defaultHeaders) {
        const textarea = document.getElementById('globalHeaders');
        if (textarea) {
            textarea.value = JSON.stringify(window.defaultHeaders, null, 2);
        }
    }
}

// 更新超时配置
function updateTimeoutConfig() {
    if (window.timeoutConfig) {
        const input = document.getElementById('timeoutSeconds');
        if (input) {
            input.value = Math.round(window.timeoutConfig.read / 1000);
        }
    }
}

// 加载安全状态
async function loadSecurityStatus() {
    try {
        const response = await fetch('/test/api/security/status');
        if (response.ok) {
            const status = await response.json();
            console.log('安全配置状态:', status);
            
            // 如果启用了令牌验证，显示提示
            if (status.enabled && status.mode !== 'ip') {
                showSecurityNotice(status);
            }
        }
    } catch (error) {
        console.warn('无法加载安全状态:', error);
    }
}

// 显示安全提示
function showSecurityNotice(status) {
    const notice = document.createElement('div');
    notice.className = 'bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-4';
    notice.innerHTML = `
        <div class="flex">
            <div class="flex-shrink-0">
                <i class="fas fa-exclamation-triangle text-yellow-400"></i>
            </div>
            <div class="ml-3">
                <p class="text-sm text-yellow-700">
                    <strong>安全提示:</strong> 当前启用了访问控制 (${status.mode} 模式)。
                    ${status.mode === 'token' || status.mode === 'both' ? '请在"访问令牌"字段中输入有效的令牌。' : ''}
                </p>
            </div>
        </div>
    `;
    
    const testContent = document.getElementById('testContent');
    if (testContent) {
        testContent.insertBefore(notice, testContent.firstChild);
    }
}

// 添加到请求历史
function addToRequestHistory(requestInfo) {
    const historyContainer = document.getElementById('requestHistory');
    const historyItem = document.createElement('div');
    
    const statusClass = requestInfo.status === 'FAILED' ? 'bg-red-100 text-red-800' : 
                       requestInfo.status >= 400 ? 'bg-yellow-100 text-yellow-800' : 
                       'bg-green-100 text-green-800';
    
    historyItem.className = 'p-2 rounded border text-xs';
    historyItem.innerHTML = `
        <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
                <span class="px-2 py-1 rounded text-white text-xs font-bold method-${requestInfo.method.toLowerCase()}">${requestInfo.method}</span>
                <span class="text-gray-600">${requestInfo.url.split('/').pop() || requestInfo.url}</span>
            </div>
            <div class="flex items-center space-x-2">
                <span class="px-2 py-1 rounded ${statusClass} text-xs">${requestInfo.status}</span>
                <span class="text-gray-500">${requestInfo.time}ms</span>
                <span class="text-gray-400">${requestInfo.timestamp}</span>
            </div>
        </div>
    `;
    
    // 限制历史记录数量
    if (historyContainer.children.length >= 10) {
        historyContainer.removeChild(historyContainer.firstChild);
    }
    
    historyContainer.appendChild(historyItem);
}

// 保存请求模板
function saveRequestTemplate() {
    const method = document.getElementById('testMethod').value;
    const url = document.getElementById('testUrl').value;
    const headers = document.getElementById('testHeaders').value;
    const body = document.getElementById('testBody').value;
    const environment = document.getElementById('testEnvironment').value;
    
    const template = {
        name: `${method} ${url}`,
        method: method,
        url: url,
        headers: headers,
        body: body,
        environment: environment,
        timestamp: new Date().toISOString()
    };
    
    // 保存到 localStorage
    const templates = JSON.parse(localStorage.getItem('requestTemplates') || '[]');
    templates.push(template);
    localStorage.setItem('requestTemplates', JSON.stringify(templates));
    
    alert('请求模板已保存！');
}

// 快速测试 API
function testApi(method, path) {
    // 切换到测试标签页
    window.apiDocsManager.switchTab('test');
    
    // 设置测试参数
    document.getElementById('testMethod').value = method;
    document.getElementById('testUrl').value = path;
    
    // 如果是需要请求体的方法，显示请求体输入框
    if (method === 'POST' || method === 'PUT' || method === 'PATCH') {
        document.getElementById('requestBodySection').classList.remove('hidden');
    } else {
        document.getElementById('requestBodySection').classList.add('hidden');
    }
    
    // 显示提示信息
    console.log(`准备测试 API: ${method} ${path}`);
}

// 全局刷新函数
function refreshData() {
    if (window.apiDocsManager) {
        window.apiDocsManager.refreshData();
    }
}

// 下载 API 文档
async function downloadDocs(format) {
    try {
        const response = await fetch(`/test/api/docs/download/${format}`);
        if (response.ok) {
            const content = await response.text();
            const filename = response.headers.get('Content-Disposition')?.split('filename=')[1]?.replace(/"/g, '') || `api-docs.${format}`;
            
            // 创建下载链接
            const blob = new Blob([content], { 
                type: response.headers.get('Content-Type') || 'text/plain' 
            });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            
            console.log(`文档下载成功: ${filename}`);
        } else {
            const errorText = await response.text();
            console.error('下载失败:', errorText);
            alert('下载失败: ' + errorText);
        }
    } catch (error) {
        console.error('下载异常:', error);
        alert('下载异常: ' + error.message);
    }
}

// 显示文档下载选项
function showDownloadOptions() {
    const options = [
        { format: 'markdown', name: 'Markdown', icon: 'fas fa-file-alt' },
        { format: 'html', name: 'HTML', icon: 'fas fa-file-code' },
        { format: 'json', name: 'OpenAPI JSON', icon: 'fas fa-file-code' }
    ];
    
    let optionsHtml = '<div class="grid grid-cols-1 gap-2">';
    options.forEach(option => {
        optionsHtml += `
            <button onclick="downloadDocs('${option.format}')" 
                    class="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-lg hover:border-blue-300 hover:bg-blue-50 transition-all duration-200">
                <div class="flex items-center">
                    <i class="${option.icon} text-blue-600 mr-3"></i>
                    <span class="font-medium text-gray-900">${option.name}</span>
                </div>
                <i class="fas fa-download text-gray-400"></i>
            </button>
        `;
    });
    optionsHtml += '</div>';
    
    // 这里可以使用模态框或下拉菜单显示选项
    // 暂时直接调用第一个选项
    downloadDocs('markdown');
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    window.apiDocsManager = new ApiDocsManager();
    loadEnvironmentConfigs(); // 加载环境配置
    loadSecurityStatus(); // 加载安全状态
});

// 性能监控相关函数
async function refreshMonitorData() {
    try {
        // 加载性能数据
        const perfResponse = await fetch('/test/api/performance');
        if (perfResponse.ok) {
            const perfData = await perfResponse.json();
            updatePerformanceUI(perfData.performance);
        }

        // 加载缓存数据
        const cacheResponse = await fetch('/test/api/cache/stats');
        if (cacheResponse.ok) {
            const cacheData = await cacheResponse.json();
            updateCacheUI(cacheData.cache);
        }
    } catch (error) {
        console.error('加载监控数据失败:', error);
    }
}

function updatePerformanceUI(performance) {
    // 更新概览数据
    document.getElementById('totalRequests').textContent = performance.totalRequests;
    document.getElementById('successfulRequests').textContent = performance.successfulRequests;
    document.getElementById('errorRequests').textContent = performance.errorRequests;
    document.getElementById('avgResponseTime').textContent = performance.averageResponseTime + 'ms';

    // 更新系统资源
    const system = performance.systemMetrics;
    document.getElementById('heapUsage').textContent = system.heapUsage.toFixed(1) + '%';
    document.getElementById('heapDetails').textContent = 
        Math.round(system.heapUsed / 1024 / 1024) + 'MB / ' + 
        Math.round(system.heapMax / 1024 / 1024) + 'MB';
    document.getElementById('heapBar').style.width = Math.min(system.heapUsage, 100) + '%';

    document.getElementById('systemLoad').textContent = system.systemLoad.toFixed(2);
    const loadPercent = Math.min(system.systemLoad * 100, 100);
    document.getElementById('loadBar').style.width = loadPercent + '%';

    document.getElementById('threadCount').textContent = system.threadCount;
    document.getElementById('peakThreadCount').textContent = system.peakThreadCount;

    // 更新端点指标
    updateEndpointMetrics(performance.endpointMetrics);
}

function updateCacheUI(cache) {
    document.getElementById('cacheUsage').textContent = cache.usagePercentage.toFixed(1) + '%';
    document.getElementById('cacheDetails').textContent = cache.currentSize + ' / ' + cache.maxSize;
    document.getElementById('cacheTtl').textContent = cache.ttlSeconds;
    document.getElementById('cacheBar').style.width = cache.usagePercentage + '%';
}

function updateEndpointMetrics(endpointMetrics) {
    const container = document.getElementById('endpointMetrics');
    if (!container) return;

    let html = '';
    Object.entries(endpointMetrics).forEach(([endpoint, metrics]) => {
        const errorRate = metrics.getErrorRate();
        const avgTime = metrics.getAverageResponseTime();
        
        html += `
            <div class="flex items-center justify-between p-3 bg-white rounded border">
                <div class="flex-1">
                    <div class="font-medium text-gray-900">${endpoint}</div>
                    <div class="text-sm text-gray-600">
                        请求: ${metrics.getRequestCount()}, 
                        错误率: ${errorRate.toFixed(1)}%, 
                        平均时间: ${avgTime.toFixed(0)}ms
                    </div>
                </div>
                <div class="flex items-center space-x-2">
                    <span class="px-2 py-1 text-xs rounded-full ${
                        errorRate < 5 ? 'bg-green-100 text-green-800' : 
                        errorRate < 20 ? 'bg-yellow-100 text-yellow-800' : 
                        'bg-red-100 text-red-800'
                    }">
                        ${errorRate.toFixed(1)}%
                    </span>
                    <span class="px-2 py-1 text-xs rounded-full ${
                        avgTime < 100 ? 'bg-green-100 text-green-800' : 
                        avgTime < 500 ? 'bg-yellow-100 text-yellow-800' : 
                        'bg-red-100 text-red-800'
                    }">
                        ${avgTime.toFixed(0)}ms
                    </span>
                </div>
            </div>
        `;
    });

    container.innerHTML = html || '<p class="text-gray-500 text-center py-4">暂无端点数据</p>';
}

async function testCache() {
    try {
        const response = await fetch('/test/api/cache/test', { method: 'POST' });
        if (response.ok) {
            const result = await response.json();
            alert(`缓存测试结果:\n命中: ${result.cacheHit}\n存储值: ${result.testValue}\n读取值: ${result.retrievedValue}`);
            refreshMonitorData(); // 刷新数据
        }
    } catch (error) {
        alert('缓存测试失败: ' + error.message);
    }
}

async function clearCache() {
    if (confirm('确定要清空所有缓存吗？')) {
        try {
            const response = await fetch('/test/api/cache/clear', { method: 'POST' });
            if (response.ok) {
                alert('缓存已清空');
                refreshMonitorData(); // 刷新数据
            }
        } catch (error) {
            alert('清空缓存失败: ' + error.message);
        }
    }
}

// 刷新告警信息
async function refreshAlerts() {
    try {
        const response = await fetch('/test/api/alerts/stats');
        if (response.ok) {
            const data = await response.json();
            updateAlertDisplay(data.alerts);
        } else {
            console.error('获取告警信息失败:', response.status);
        }
    } catch (error) {
        console.error('刷新告警异常:', error);
    }
}

// 更新告警显示
function updateAlertDisplay(alerts) {
    const criticalAlerts = document.getElementById('criticalAlerts');
    const warningAlerts = document.getElementById('warningAlerts');
    
    if (criticalAlerts && warningAlerts) {
        criticalAlerts.textContent = alerts.criticalAlerts || 0;
        warningAlerts.textContent = alerts.warningAlerts || 0;
        
        // 根据告警数量设置颜色
        if (alerts.criticalAlerts > 0) {
            criticalAlerts.classList.add('text-red-600');
        } else {
            criticalAlerts.classList.remove('text-red-600');
        }
        
        if (alerts.warningAlerts > 0) {
            warningAlerts.classList.add('text-yellow-600');
        } else {
            warningAlerts.classList.remove('text-yellow-600');
        }
    }
}

// 刷新系统健康状态
async function refreshHealthStatus() {
    try {
        const response = await fetch('/test/api/health');
        if (response.ok) {
            const data = await response.json();
            updateHealthDisplay(data);
        } else {
            console.error('获取健康状态失败:', response.status);
        }
    } catch (error) {
        console.error('刷新健康状态异常:', error);
    }
}

// 更新健康状态显示
function updateHealthDisplay(healthData) {
    const healthScore = document.getElementById('healthScore');
    const healthStatusText = document.getElementById('healthStatusText');
    const alertCount = document.getElementById('alertCount');
    
    if (healthScore && healthStatusText && alertCount) {
        // 更新健康分数
        healthScore.textContent = healthData.healthScore || 0;
        
        // 更新状态文本和颜色
        const status = healthData.status || 'UNKNOWN';
        healthStatusText.textContent = status;
        healthStatusText.className = 'px-2 py-1 rounded-full text-sm font-medium';
        
        if (status === 'HEALTHY') {
            healthStatusText.classList.add('bg-green-100', 'text-green-800');
        } else if (status === 'WARNING') {
            healthStatusText.classList.add('bg-yellow-100', 'text-yellow-800');
        } else if (status === 'CRITICAL') {
            healthStatusText.classList.add('bg-red-100', 'text-red-800');
        } else {
            healthStatusText.classList.add('bg-gray-100', 'text-gray-800');
        }
        
        // 更新告警总数
        const totalAlerts = (healthData.alerts?.criticalAlerts || 0) + (healthData.alerts?.warningAlerts || 0);
        alertCount.textContent = totalAlerts;
        
        // 根据告警数量设置颜色
        if (totalAlerts > 0) {
            alertCount.classList.add('text-red-600');
        } else {
            alertCount.classList.remove('text-red-600');
        }
    }
}

// 增强的刷新数据函数
async function refreshData() {
    try {
        // 显示加载状态
        showLoading(true);
        
        // 并行获取所有数据
        const [apiResponse, performanceResponse, cacheResponse, healthResponse, alertsResponse] = await Promise.all([
            fetch('/test/api/docs'),
            fetch('/test/api/performance'),
            fetch('/test/api/cache/stats'),
            fetch('/test/api/health'),
            fetch('/test/api/alerts/stats')
        ]);
        
        // 处理API文档数据
        if (apiResponse.ok) {
            const data = await apiResponse.json();
            updateApiDocsDisplay(data);
        }
        
        // 处理性能数据
        if (performanceResponse.ok) {
            const data = await performanceResponse.json();
            updatePerformanceDisplay(data);
        }
        
        // 处理缓存数据
        if (cacheResponse.ok) {
            const data = await cacheResponse.json();
            updateCacheDisplay(data);
        }
        
        // 处理健康状态数据
        if (healthResponse.ok) {
            const data = await healthResponse.json();
            updateHealthDisplay(data);
        }
        
        // 处理告警数据
        if (alertsResponse.ok) {
            const data = await alertsResponse.json();
            updateAlertDisplay(data.alerts);
        }
        
        showLoading(false);
        console.log('所有数据刷新完成');
        
    } catch (error) {
        console.error('刷新数据异常:', error);
        showLoading(false);
        alert('刷新数据失败: ' + error.message);
    }
}
