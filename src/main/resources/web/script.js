// JDeps Analysis Report JavaScript

// Sample data - this would normally come from the JDeps analysis
let analysisData = {
    modules: [
        { name: 'java.base', type: 'jdk', dependencies: 0, classes: 45 },
        { name: 'java.time', type: 'jdk', dependencies: 1, classes: 8 },
        { name: 'java.util.concurrent', type: 'jdk', dependencies: 1, classes: 5 },
        { name: 'java.util.stream', type: 'jdk', dependencies: 1, classes: 12 },
        { name: 'java.nio.file', type: 'jdk', dependencies: 1, classes: 7 },
        { name: 'java.util.regex', type: 'jdk', dependencies: 1, classes: 3 },
        { name: 'org.apache.commons.lang3', type: 'external', dependencies: 2, classes: 15 },
        { name: 'com.fasterxml.jackson.databind', type: 'external', dependencies: 3, classes: 25 }
    ],
    dependencies: [
        {
            source: 'com.example.jdeps.JDepsTestApp',
            target: 'java.base',
            type: 'jdk',
            details: 'Uses System.out, String, Map, List, etc.'
        },
        {
            source: 'com.example.jdeps.JDepsTestApp',
            target: 'java.time',
            type: 'jdk',
            details: 'Uses LocalDateTime for timestamp operations'
        },
        {
            source: 'com.example.jdeps.JDepsTestApp',
            target: 'java.util.concurrent',
            type: 'jdk',
            details: 'Uses ConcurrentHashMap for thread-safe operations'
        },
        {
            source: 'com.example.jdeps.JDepsTestApp',
            target: 'java.util.stream',
            type: 'jdk',
            details: 'Uses Stream API for functional programming'
        },
        {
            source: 'com.example.jdeps.JDepsTestApp',
            target: 'org.apache.commons.lang3',
            type: 'external',
            details: 'Uses StringUtils for string manipulation'
        },
        {
            source: 'com.example.jdeps.JDepsTestApp',
            target: 'com.fasterxml.jackson.databind',
            type: 'external',
            details: 'Uses ObjectMapper for JSON processing'
        },
        {
            source: 'com.example.jdeps.DependencyTestUtil',
            target: 'java.nio.file',
            type: 'jdk',
            details: 'Uses Files and Path for file operations'
        },
        {
            source: 'com.example.jdeps.DependencyTestUtil',
            target: 'java.util.regex',
            type: 'jdk',
            details: 'Uses Pattern and Matcher for regex operations'
        }
    ],
    classes: [
        {
            name: 'com.example.jdeps.JDepsTestApp',
            module: 'jdeps-test',
            dependencies: 6,
            issues: 0
        },
        {
            name: 'com.example.jdeps.DependencyTestUtil',
            module: 'jdeps-test',
            dependencies: 4,
            issues: 0
        },
        {
            name: 'com.example.jdeps.SimpleJDepsTestApp',
            module: 'jdeps-test',
            dependencies: 8,
            issues: 0
        }
    ],
    issues: [
        {
            type: 'info',
            message: 'No JDK internal API usage detected',
            details: 'All dependencies use public APIs'
        }
    ],
    recommendations: [
        {
            type: 'optimization',
            message: 'Consider using java.util.stream for more operations',
            details: 'Stream API can simplify collection processing code'
        },
        {
            type: 'modularity',
            message: 'Good modular design detected',
            details: 'Dependencies are well-organized and follow good practices'
        }
    ]
};

// Chart instances
let moduleChart = null;
let dependencyChart = null;

// D3.js graph variables
let svg, simulation, nodes, links;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeData();
    renderCharts();
    renderDependencies();
    renderModules();
    renderGraph();
    renderDetails();
    
    // Set analysis date
    document.getElementById('analysisDate').textContent = new Date().toLocaleString();
});

// Tab functionality
function showTab(tabName) {
    // Hide all tab contents
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    // Remove active class from all tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab content
    document.getElementById(tabName).classList.add('active');
    
    // Add active class to clicked tab button
    event.target.classList.add('active');
    
    // Re-render charts if overview tab is selected
    if (tabName === 'overview') {
        setTimeout(renderCharts, 100);
    }
    
    // Re-render graph if graph tab is selected
    if (tabName === 'graph') {
        setTimeout(renderGraph, 100);
    }
}

// Initialize overview data
function initializeData() {
    const totalModules = analysisData.modules.length;
    const totalDependencies = analysisData.dependencies.length;
    const internalAPIs = analysisData.issues.filter(issue => issue.type === 'internal-api').length;
    const totalClasses = analysisData.classes.length;
    
    document.getElementById('totalModules').textContent = totalModules;
    document.getElementById('totalDependencies').textContent = totalDependencies;
    document.getElementById('internalAPIs').textContent = internalAPIs;
    document.getElementById('totalClasses').textContent = totalClasses;
}

// Render charts
function renderCharts() {
    renderModuleChart();
    renderDependencyChart();
}

function renderModuleChart() {
    const ctx = document.getElementById('moduleChart').getContext('2d');
    
    // Destroy existing chart if it exists
    if (moduleChart) {
        moduleChart.destroy();
    }
    
    const moduleTypes = analysisData.modules.reduce((acc, module) => {
        acc[module.type] = (acc[module.type] || 0) + 1;
        return acc;
    }, {});
    
    moduleChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: Object.keys(moduleTypes).map(type => type.toUpperCase()),
            datasets: [{
                data: Object.values(moduleTypes),
                backgroundColor: ['#3498db', '#e74c3c', '#27ae60', '#f39c12'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 20,
                        usePointStyle: true
                    }
                }
            }
        }
    });
}

function renderDependencyChart() {
    const ctx = document.getElementById('dependencyChart').getContext('2d');
    
    // Destroy existing chart if it exists
    if (dependencyChart) {
        dependencyChart.destroy();
    }
    
    const dependencyTypes = analysisData.dependencies.reduce((acc, dep) => {
        acc[dep.type] = (acc[dep.type] || 0) + 1;
        return acc;
    }, {});
    
    dependencyChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: Object.keys(dependencyTypes).map(type => type.toUpperCase()),
            datasets: [{
                label: 'Dependencies',
                data: Object.values(dependencyTypes),
                backgroundColor: ['#3498db', '#e74c3c', '#27ae60'],
                borderRadius: 8,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0,0,0,0.1)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

// Render dependencies list
function renderDependencies() {
    const container = document.getElementById('dependenciesList');
    container.innerHTML = '';
    
    analysisData.dependencies.forEach(dep => {
        const item = document.createElement('div');
        item.className = 'dependency-item';
        
        item.innerHTML = `
            <div class="dependency-info">
                <div class="dependency-icon ${dep.type}">
                    <i class="fas ${getIconForType(dep.type)}"></i>
                </div>
                <div class="dependency-details">
                    <h4>${dep.source} → ${dep.target}</h4>
                    <p>${dep.details}</p>
                </div>
            </div>
            <div class="dependency-badge badge-${dep.type}">
                ${dep.type.toUpperCase()}
            </div>
        `;
        
        container.appendChild(item);
    });
    
    // Add filter functionality
    const filterInput = document.getElementById('dependencyFilter');
    const typeFilter = document.getElementById('dependencyTypeFilter');
    
    function filterDependencies() {
        const searchTerm = filterInput.value.toLowerCase();
        const selectedType = typeFilter.value;
        
        const items = container.querySelectorAll('.dependency-item');
        items.forEach(item => {
            const text = item.textContent.toLowerCase();
            const matchesSearch = text.includes(searchTerm);
            const matchesType = selectedType === 'all' || text.includes(selectedType);
            
            item.style.display = matchesSearch && matchesType ? 'flex' : 'none';
        });
    }
    
    filterInput.addEventListener('input', filterDependencies);
    typeFilter.addEventListener('change', filterDependencies);
}

// Render modules grid
function renderModules() {
    const container = document.getElementById('modulesGrid');
    container.innerHTML = '';
    
    analysisData.modules.forEach(module => {
        const card = document.createElement('div');
        card.className = 'module-card';
        
        card.innerHTML = `
            <div class="module-header">
                <div class="module-icon">
                    <i class="fas ${getIconForType(module.type)}"></i>
                </div>
                <div class="module-name">${module.name}</div>
            </div>
            <p>Type: <strong>${module.type.toUpperCase()}</strong></p>
            <div class="module-stats">
                <div class="module-stat">
                    <div class="number">${module.dependencies}</div>
                    <div class="label">Dependencies</div>
                </div>
                <div class="module-stat">
                    <div class="number">${module.classes}</div>
                    <div class="label">Classes</div>
                </div>
            </div>
        `;
        
        container.appendChild(card);
    });
}

// Render dependency graph using D3.js
function renderGraph() {
    const container = document.getElementById('dependencyGraph');
    container.innerHTML = '';
    
    const width = container.clientWidth;
    const height = container.clientHeight;
    
    svg = d3.select('#dependencyGraph')
        .append('svg')
        .attr('width', width)
        .attr('height', height);
    
    // Create nodes and links for D3
    const graphNodes = [];
    const graphLinks = [];
    const nodeMap = new Map();
    
    // Add all unique nodes
    analysisData.dependencies.forEach(dep => {
        if (!nodeMap.has(dep.source)) {
            nodeMap.set(dep.source, { id: dep.source, type: 'source', group: 1 });
            graphNodes.push(nodeMap.get(dep.source));
        }
        if (!nodeMap.has(dep.target)) {
            nodeMap.set(dep.target, { id: dep.target, type: dep.type, group: dep.type === 'jdk' ? 2 : 3 });
            graphNodes.push(nodeMap.get(dep.target));
        }
    });
    
    // Add links
    analysisData.dependencies.forEach(dep => {
        graphLinks.push({
            source: dep.source,
            target: dep.target,
            type: dep.type
        });
    });
    
    // Create force simulation
    simulation = d3.forceSimulation(graphNodes)
        .force('link', d3.forceLink(graphLinks).id(d => d.id).distance(100))
        .force('charge', d3.forceManyBody().strength(-300))
        .force('center', d3.forceCenter(width / 2, height / 2));
    
    // Add links
    const link = svg.append('g')
        .selectAll('line')
        .data(graphLinks)
        .enter()
        .append('line')
        .attr('stroke', d => getColorForType(d.type))
        .attr('stroke-width', 2)
        .attr('stroke-opacity', 0.8);
    
    // Add nodes
    const node = svg.append('g')
        .selectAll('circle')
        .data(graphNodes)
        .enter()
        .append('circle')
        .attr('r', d => d.type === 'source' ? 8 : 6)
        .attr('fill', d => getColorForType(d.type))
        .call(d3.drag()
            .on('start', dragstarted)
            .on('drag', dragged)
            .on('end', dragended));
    
    // Add labels
    const label = svg.append('g')
        .selectAll('text')
        .data(graphNodes)
        .enter()
        .append('text')
        .text(d => d.id.split('.').pop())
        .attr('font-size', '10px')
        .attr('dx', 12)
        .attr('dy', 4);
    
    // Add tooltips
    node.append('title')
        .text(d => d.id);
    
    simulation.on('tick', () => {
        link
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);
        
        node
            .attr('cx', d => d.x)
            .attr('cy', d => d.y);
        
        label
            .attr('x', d => d.x)
            .attr('y', d => d.y);
    });
    
    function dragstarted(event, d) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }
    
    function dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }
    
    function dragended(event, d) {
        if (!event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
}

// Render detailed analysis
function renderDetails() {
    renderClassAnalysis();
    renderIssues();
    renderRecommendations();
}

function renderClassAnalysis() {
    const container = document.getElementById('classAnalysis');
    container.innerHTML = '';
    
    analysisData.classes.forEach(cls => {
        const item = document.createElement('div');
        item.className = 'class-item';
        
        item.innerHTML = `
            <h4>${cls.name}</h4>
            <p><strong>Module:</strong> ${cls.module}</p>
            <p><strong>Dependencies:</strong> ${cls.dependencies}</p>
            <p><strong>Issues:</strong> ${cls.issues}</p>
        `;
        
        container.appendChild(item);
    });
}

function renderIssues() {
    const container = document.getElementById('issuesList');
    container.innerHTML = '';
    
    if (analysisData.issues.length === 0) {
        container.innerHTML = '<div class="issue-item">No issues detected ✓</div>';
        return;
    }
    
    analysisData.issues.forEach(issue => {
        const item = document.createElement('div');
        item.className = 'issue-item';
        
        item.innerHTML = `
            <h4><i class="fas fa-exclamation-triangle"></i> ${issue.message}</h4>
            <p>${issue.details}</p>
        `;
        
        container.appendChild(item);
    });
}

function renderRecommendations() {
    const container = document.getElementById('recommendationsList');
    container.innerHTML = '';
    
    analysisData.recommendations.forEach(rec => {
        const item = document.createElement('div');
        item.className = 'recommendation-item';
        
        item.innerHTML = `
            <h4><i class="fas fa-lightbulb"></i> ${rec.message}</h4>
            <p>${rec.details}</p>
        `;
        
        container.appendChild(item);
    });
}

// Utility functions
function getIconForType(type) {
    switch (type) {
        case 'jdk': return 'fa-cube';
        case 'external': return 'fa-external-link-alt';
        case 'internal': return 'fa-home';
        case 'source': return 'fa-file-code';
        default: return 'fa-question';
    }
}

function getColorForType(type) {
    switch (type) {
        case 'jdk': return '#f39c12';
        case 'external': return '#e74c3c';
        case 'internal': return '#27ae60';
        case 'source': return '#3498db';
        default: return '#95a5a6';
    }
}

// Graph control functions
function zoomIn() {
    if (svg) {
        svg.transition().duration(300).attr('transform', 'scale(1.2)');
    }
}

function zoomOut() {
    if (svg) {
        svg.transition().duration(300).attr('transform', 'scale(0.8)');
    }
}

function resetZoom() {
    if (svg) {
        svg.transition().duration(300).attr('transform', 'scale(1)');
    }
}

function changeLayout() {
    const layout = document.getElementById('graphLayout').value;
    // Implement different layout algorithms here
    console.log('Changing layout to:', layout);
}

// Action functions
function refreshAnalysis() {
    showLoading();
    
    // Simulate API call to refresh analysis
    setTimeout(() => {
        hideLoading();
        // Reload data and re-render everything
        initializeData();
        renderCharts();
        renderDependencies();
        renderModules();
        renderGraph();
        renderDetails();
        
        // Update analysis date
        document.getElementById('analysisDate').textContent = new Date().toLocaleString();
    }, 2000);
}

function exportReport() {
    // Create a comprehensive report
    const report = {
        timestamp: new Date().toISOString(),
        project: document.getElementById('projectName').textContent,
        summary: {
            totalModules: document.getElementById('totalModules').textContent,
            totalDependencies: document.getElementById('totalDependencies').textContent,
            internalAPIs: document.getElementById('internalAPIs').textContent,
            totalClasses: document.getElementById('totalClasses').textContent
        },
        data: analysisData
    };
    
    // Download as JSON
    const blob = new Blob([JSON.stringify(report, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'jdeps-analysis-report.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

function showLoading() {
    document.getElementById('loadingOverlay').classList.add('show');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.remove('show');
}

// Responsive chart resize
window.addEventListener('resize', () => {
    if (moduleChart) moduleChart.resize();
    if (dependencyChart) dependencyChart.resize();
    
    // Re-render graph with new dimensions
    if (svg) {
        setTimeout(renderGraph, 100);
    }
});
