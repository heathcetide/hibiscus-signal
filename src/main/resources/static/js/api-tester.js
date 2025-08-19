/**
 * API接口测试器
 * 提供接口测试功能
 */

class ApiTester {
    constructor() {
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadApiList();
    }

    bindEvents() {
        // 绑定测试按钮事件
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('test-api-btn')) {
                const apiCard = e.target.closest('.api-card');
                const endpoint = apiCard.dataset.endpoint;
                const method = apiCard.dataset.method;
                this.showTestModal(endpoint, method);
            }
        });

        // 绑定批量测试按钮事件
        const batchTestBtn = document.getElementById('batchTestBtn');
        if (batchTestBtn) {
            batchTestBtn.addEventListener('click', () => this.batchTest());
        }
    }

    /**
     * 加载API列表
     */
    async loadApiList() {
        try {
            const response = await fetch('/test/api/docs');
            if (response.ok) {
                const data = await response.json();
                this.renderApiList(data);
            }
        } catch (error) {
            console.error('加载API列表失败:', error);
        }
    }

    /**
     * 渲染API列表
     */
    renderApiList(data) {
        const apiListContainer = document.getElementById('apiList');
        if (!apiListContainer) return;

        if (!data || !data.controllers) {
            apiListContainer.innerHTML = '<p class="text-gray-500">暂无API数据</p>';
            return;
        }

        let html = '';
        data.controllers.forEach(controller => {
            html += `
                <div class="bg-white rounded-lg shadow-md p-6 mb-6">
                    <h3 class="text-lg font-semibold text-gray-800 mb-4 flex items-center">
                        <i class="fas fa-cube mr-2 text-blue-500"></i>${controller.name}
                    </h3>
                    <div class="space-y-4">
            `;

            controller.apis.forEach(api => {
                const methodColor = this.getMethodColor(api.method);
                html += `
                    <div class="api-card border border-gray-200 rounded-lg p-4" 
                         data-endpoint="${api.path}" 
                         data-method="${api.method}">
                        <div class="flex items-center justify-between mb-3">
                            <div class="flex items-center space-x-3">
                                <span class="method-badge method-${api.method.toLowerCase()} px-3 py-1 rounded-full text-sm font-medium text-white">
                                    ${api.method}
                                </span>
                                <span class="text-gray-700 font-mono">${api.path}</span>
                            </div>
                            <button class="test-api-btn bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm transition-colors">
                                <i class="fas fa-play mr-2"></i>测试接口
                            </button>
                        </div>
                        <div class="text-gray-600 text-sm mb-3">
                            ${api.description || '无描述'}
                        </div>
                        <div class="flex items-center space-x-4 text-xs text-gray-500">
                            <span>参数: ${api.parameters ? api.parameters.length : 0}</span>
                            <span>响应: ${api.responseType || '未知'}</span>
                        </div>
                    </div>
                `;
            });

            html += `
                    </div>
                </div>
            `;
        });

        apiListContainer.innerHTML = html;
    }

    /**
     * 获取HTTP方法对应的颜色
     */
    getMethodColor(method) {
        const colors = {
            'GET': 'bg-green-500',
            'POST': 'bg-blue-500',
            'PUT': 'bg-yellow-500',
            'DELETE': 'bg-red-500',
            'PATCH': 'bg-purple-500'
        };
        return colors[method] || 'bg-gray-500';
    }

    /**
     * 显示测试弹窗
     */
    showTestModal(endpoint, method) {
        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50';
        modal.innerHTML = `
            <div class="bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto">
                <div class="flex items-center justify-between p-6 border-b border-gray-200">
                    <h3 class="text-lg font-semibold text-gray-800">
                        <span class="method-badge method-${method.toLowerCase()} px-2 py-1 rounded text-sm font-medium text-white mr-3">
                            ${method}
                        </span>
                        测试接口: ${endpoint}
                    </h3>
                    <button class="close-modal text-gray-400 hover:text-gray-600 text-2xl">&times;</button>
                </div>
                
                <div class="p-6">
                    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <!-- 参数配置 -->
                        <div>
                            <h4 class="text-md font-semibold text-gray-800 mb-4">参数配置</h4>
                            <div id="parameterConfig" class="space-y-4">
                                <!-- 参数配置将在这里动态生成 -->
                            </div>
                        </div>
                        
                        <!-- 测试结果 -->
                        <div>
                            <h4 class="text-md font-semibold text-gray-800 mb-4">测试结果</h4>
                            <div id="testResult" class="space-y-4">
                                <div class="text-gray-500 text-center py-8">
                                    <i class="fas fa-play-circle text-4xl mb-4"></i>
                                    <p>点击执行测试按钮开始测试</p>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="flex justify-end space-x-3 mt-6 pt-6 border-t border-gray-200">
                        <button class="px-4 py-2 bg-gray-300 hover:bg-gray-400 text-gray-700 rounded-lg transition-colors">
                            取消
                        </button>
                        <button class="execute-test-btn px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors">
                            <i class="fas fa-play mr-2"></i>执行测试
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // 绑定关闭事件
        const closeBtn = modal.querySelector('.close-modal');
        const cancelBtn = modal.querySelector('.button:first-child');
        [closeBtn, cancelBtn].forEach(btn => {
            if (btn) {
                btn.addEventListener('click', () => {
                    document.body.removeChild(modal);
                });
            }
        });

        // 绑定执行测试事件
        const executeBtn = modal.querySelector('.execute-test-btn');
        if (executeBtn) {
            executeBtn.addEventListener('click', () => this.executeTest(endpoint, method, modal));
        }

        // 加载参数配置
        this.loadParameterConfig(endpoint, method, modal);
    }

    /**
     * 加载参数配置
     */
    async loadParameterConfig(endpoint, method, modal) {
        try {
            // 这里应该调用后端API获取参数信息
            // 暂时使用模拟数据
            const parameters = this.getMockParameters(endpoint, method);
            this.renderParameterConfig(parameters, modal);
        } catch (error) {
            console.error('加载参数配置失败:', error);
        }
    }

    /**
     * 获取模拟参数数据
     */
    getMockParameters(endpoint, method) {
        // 根据端点和方法返回不同的参数配置
        if (endpoint.includes('/users/') && method === 'GET') {
            return [
                {
                    name: 'id',
                    type: 'number',
                    description: '用户ID',
                    defaultValue: '1',
                    required: true,
                    example: '123'
                },
                {
                    name: 'includeProfile',
                    type: 'boolean',
                    description: '是否包含用户资料',
                    defaultValue: 'false',
                    required: false,
                    example: 'true'
                }
            ];
        } else if (endpoint.includes('/users/search') && method === 'GET') {
            return [
                {
                    name: 'keyword',
                    type: 'string',
                    description: '搜索关键词',
                    defaultValue: '',
                    required: true,
                    example: '张三'
                },
                {
                    name: 'page',
                    type: 'number',
                    description: '页码',
                    defaultValue: '1',
                    required: false,
                    example: '1'
                },
                {
                    name: 'size',
                    type: 'number',
                    description: '每页大小',
                    defaultValue: '10',
                    required: false,
                    example: '20'
                }
            ];
        } else if (endpoint.includes('/users') && method === 'POST') {
            return [
                {
                    name: 'name',
                    type: 'string',
                    description: '用户姓名',
                    defaultValue: '',
                    required: true,
                    example: '张三'
                },
                {
                    name: 'email',
                    type: 'string',
                    description: '用户邮箱',
                    defaultValue: '',
                    required: true,
                    example: 'zhangsan@example.com'
                },
                {
                    name: 'age',
                    type: 'number',
                    description: '用户年龄',
                    defaultValue: '',
                    required: false,
                    example: '25'
                }
            ];
        }

        return [];
    }

    /**
     * 渲染参数配置
     */
    renderParameterConfig(parameters, modal) {
        const container = modal.querySelector('#parameterConfig');
        if (!container) return;

        if (parameters.length === 0) {
            container.innerHTML = '<p class="text-gray-500 text-center py-4">该接口无需参数</p>';
            return;
        }

        let html = '';
        parameters.forEach(param => {
            html += `
                <div class="space-y-2">
                    <label class="block text-sm font-medium text-gray-700">
                        ${param.name}
                        ${param.required ? '<span class="text-red-500">*</span>' : ''}
                    </label>
                    <input type="text" 
                           class="param-input w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                           data-param="${param.name}"
                           placeholder="${param.example || param.description}"
                           value="${param.defaultValue || ''}"
                           ${param.required ? 'required' : ''}>
                    <div class="text-xs text-gray-500">
                        ${param.description}
                        ${param.example ? `(示例: ${param.example})` : ''}
                    </div>
                </div>
            `;
        });

        container.innerHTML = html;
    }

    /**
     * 执行测试
     */
    async executeTest(endpoint, method, modal) {
        const resultContainer = modal.querySelector('#testResult');
        if (!resultContainer) return;

        // 显示加载状态
        resultContainer.innerHTML = `
            <div class="text-center py-8">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <p class="text-gray-600">正在执行测试...</p>
            </div>
        `;

        try {
            // 收集参数
            const parameters = this.collectParameters(modal);
            
            // 执行测试
            const testRequest = {
                endpoint: endpoint,
                method: method,
                parameters: parameters
            };

            const response = await fetch('/test/api/test', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(testRequest)
            });

            const result = await response.json();
            this.displayTestResult(result, resultContainer);

        } catch (error) {
            console.error('测试执行失败:', error);
            resultContainer.innerHTML = `
                <div class="text-center py-8 text-red-600">
                    <i class="fas fa-exclamation-triangle text-4xl mb-4"></i>
                    <p>测试执行失败: ${error.message}</p>
                </div>
            `;
        }
    }

    /**
     * 收集参数
     */
    collectParameters(modal) {
        const parameters = {};
        const inputs = modal.querySelectorAll('.param-input');
        
        inputs.forEach(input => {
            const paramName = input.dataset.param;
            const value = input.value.trim();
            
            if (value !== '') {
                parameters[paramName] = value;
            }
        });

        return parameters;
    }

    /**
     * 显示测试结果
     */
    displayTestResult(result, container) {
        if (!result.success) {
            container.innerHTML = `
                <div class="text-center py-8 text-red-600">
                    <i class="fas fa-times-circle text-4xl mb-4"></i>
                    <p>测试失败: ${result.error || '未知错误'}</p>
                </div>
            `;
            return;
        }

        const testResult = result.testResult;
        const statusClass = testResult.success ? 'text-green-600' : 'text-red-600';
        const statusIcon = testResult.success ? 'fa-check-circle' : 'fa-times-circle';

        container.innerHTML = `
            <div class="space-y-4">
                <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                    <div class="flex items-center space-x-3">
                        <i class="fas ${statusIcon} text-2xl ${statusClass}"></i>
                        <div>
                            <div class="font-medium ${statusClass}">
                                ${testResult.success ? '测试成功' : '测试失败'}
                            </div>
                            <div class="text-sm text-gray-600">
                                状态码: ${testResult.statusCode} | 耗时: ${testResult.duration}ms
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="space-y-3">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">请求参数</label>
                        <pre class="bg-gray-50 p-3 rounded-lg text-sm overflow-x-auto">${JSON.stringify(testResult.parameters, null, 2)}</pre>
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">响应内容</label>
                        <pre class="bg-gray-50 p-3 rounded-lg text-sm overflow-x-auto">${testResult.responseBody || '无响应内容'}</pre>
                    </div>
                    
                    ${testResult.errorMessage ? `
                        <div>
                            <label class="block text-sm font-medium text-red-700 mb-2">错误信息</label>
                            <div class="bg-red-50 p-3 rounded-lg text-red-700 text-sm">${testResult.errorMessage}</div>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    /**
     * 批量测试
     */
    async batchTest() {
        // 这里可以实现批量测试逻辑
        console.log('批量测试功能待实现');
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    window.apiTester = new ApiTester();
});
