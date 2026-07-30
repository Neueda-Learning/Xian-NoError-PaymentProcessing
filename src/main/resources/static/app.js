const API_BASE = '/api/payments';

// DOM Elements
const createPage = document.getElementById('createPage');
const listPage = document.getElementById('listPage');
const detailSidebar = document.getElementById('detailSidebar');
const paymentsTableBody = document.getElementById('paymentsTableBody');
const statusFilter = document.getElementById('statusFilter');
const refreshBtn = document.getElementById('refreshBtn');
const createPaymentForm = document.getElementById('createPaymentForm');
const formMessage = document.getElementById('formMessage');
const paymentDetail = document.getElementById('paymentDetail');
const historyTimeline = document.getElementById('historyTimeline');
const actionButtons = document.getElementById('actionButtons');
const fillDemoBtn = document.getElementById('fillDemoBtn');
const resetFormBtn = document.getElementById('resetFormBtn');
const goToCreateBtn = document.getElementById('goToCreateBtn');
const goToListBtn = document.getElementById('goToListBtn');
const closeSidebarBtn = document.getElementById('closeSidebarBtn');

// State
let selectedPaymentId = null;
let allPayments = [];
let sortConfig = {
    field: null,
    direction: null // 'asc', 'desc', or null
};

document.addEventListener('DOMContentLoaded', () => {
    setupRouting();
    setupEventListeners();
    navigateTo('list');
});

function setupEventListeners() {
    refreshBtn.addEventListener('click', loadPayments);
    statusFilter.addEventListener('change', loadPayments);
    createPaymentForm.addEventListener('submit', createPayment);
    fillDemoBtn.addEventListener('click', fillDemoData);
    resetFormBtn.addEventListener('click', () => {
        createPaymentForm.reset();
        hideMessage();
    });
    goToCreateBtn.addEventListener('click', () => navigateTo('create'));
    goToListBtn.addEventListener('click', () => navigateTo('list'));
    closeSidebarBtn.addEventListener('click', closeSidebar);

    // Setup table header sorting
    document.querySelectorAll('th[data-sort]').forEach(header => {
        header.addEventListener('click', () => {
            const field = header.dataset.sort;
            handleHeaderClick(field);
        });
    });
}

function setupRouting() {
    window.addEventListener('hashchange', () => {
        const hash = window.location.hash.slice(1) || 'list';
        navigateTo(hash);
    });
}

function navigateTo(page) {
    window.location.hash = page;

    // Update page visibility
    createPage.classList.toggle('active', page === 'create');
    createPage.classList.toggle('hidden', page !== 'create');
    listPage.classList.toggle('active', page === 'list');
    listPage.classList.toggle('hidden', page !== 'list');

    // Update nav buttons
    goToCreateBtn.style.display = page === 'list' ? 'block' : 'none';
    goToListBtn.style.display = page === 'create' ? 'block' : 'none';

    // Close sidebar when navigating away
    if (page === 'create') {
        closeSidebar();
    }

    // Load payments on list page
    if (page === 'list') {
        loadPayments();
    }
}

async function loadPayments() {
    const status = statusFilter.value;
    const url = status ? `${API_BASE}?status=${status}` : API_BASE;

    try {
        allPayments = await fetchJson(url);
        renderPayments(allPayments);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderPayments(payments) {
    paymentsTableBody.innerHTML = '';

    if (!payments.length) {
        paymentsTableBody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; color: #667085; padding: 20px;">No payments found.</td>
            </tr>
        `;
        return;
    }

    for (const payment of payments) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${payment.id}</td>
            <td>${escapeHtml(payment.reference || '-')}</td>
            <td>${payment.amount}</td>
            <td>${payment.currency}</td>
            <td>${escapeHtml(payment.sourceAccount)}</td>
            <td>${escapeHtml(payment.destinationAccount)}</td>
            <td>${statusBadge(payment.status)}</td>
            <td>${formatDate(payment.createdAt)}</td>
        `;
        row.addEventListener('click', () => {
            loadPaymentDetails(payment.id);
            openSidebar();
        });
        paymentsTableBody.appendChild(row);
    }
}

function handleHeaderClick(field) {
    // Determine sort direction
    if (sortConfig.field === field) {
        // Toggle direction or clear
        if (sortConfig.direction === 'asc') {
            sortConfig.direction = 'desc';
        } else if (sortConfig.direction === 'desc') {
            sortConfig.field = null;
            sortConfig.direction = null;
        } else {
            sortConfig.direction = 'asc';
        }
    } else {
        sortConfig.field = field;
        sortConfig.direction = 'asc';
    }

    // Update sort indicators
    document.querySelectorAll('.sort-indicator').forEach(ind => {
        ind.classList.remove('asc', 'desc');
    });

    if (sortConfig.field) {
        const activeHeader = document.querySelector(`th[data-sort="${sortConfig.field}"]`);
        if (activeHeader) {
            const indicator = activeHeader.querySelector('.sort-indicator');
            if (indicator) {
                indicator.classList.add(sortConfig.direction);
            }
        }

        // Sort payments
        const sortedPayments = [...allPayments].sort((a, b) => {
            let aVal = a[sortConfig.field];
            let bVal = b[sortConfig.field];

            // Handle numeric comparisons
            if (typeof aVal === 'string' && !isNaN(aVal)) {
                aVal = parseFloat(aVal);
                bVal = parseFloat(bVal);
            }

            // Handle date comparisons
            if (sortConfig.field === 'createdAt') {
                aVal = new Date(aVal).getTime();
                bVal = new Date(bVal).getTime();
            }

            if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
            if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
            return 0;
        });

        renderPayments(sortedPayments);
    } else {
        renderPayments(allPayments);
    }
}

async function createPayment(event) {
    event.preventDefault();

    const request = {
        idempotencyKey: document.getElementById('idempotencyKey').value.trim(),
        sourceAccount: document.getElementById('sourceAccount').value.trim(),
        destinationAccount: document.getElementById('destinationAccount').value.trim(),
        amount: Number(document.getElementById('amount').value),
        currency: document.getElementById('currency').value,
        reference: document.getElementById('reference').value.trim()
    };

    if (request.sourceAccount === request.destinationAccount) {
        showMessage('Source account and destination account must be different.', 'error');
        return;
    }

    try {
        const createdPayment = await fetchJson(API_BASE, {
            method: 'POST',
            body: JSON.stringify(request)
        });

        showMessage(`Payment #${createdPayment.id} created successfully.`, 'success');
        createPaymentForm.reset();
        await navigateTo('list');
        await loadPaymentDetails(createdPayment.id);
        openSidebar();
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function loadPaymentDetails(id) {
    selectedPaymentId = id;

    try {
        const details = await fetchJson(`${API_BASE}/${id}/details`);
        renderPaymentDetail(details.payment);
        renderActions(details.payment);
        renderHistory(details.history);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderPaymentDetail(payment) {
    paymentDetail.classList.remove('empty');

    paymentDetail.innerHTML = `
        <div class="detail-item">
            <span>Payment ID</span>
            <strong>${payment.id}</strong>
        </div>
        <div class="detail-item">
            <span>Status</span>
            <strong>${statusBadge(payment.status)}</strong>
        </div>
        <div class="detail-item">
            <span>Amount</span>
            <strong>${payment.amount} ${payment.currency}</strong>
        </div>
        <div class="detail-item">
            <span>Idempotency Key</span>
            <strong>${escapeHtml(payment.idempotencyKey)}</strong>
        </div>
        <div class="detail-item">
            <span>Source Account</span>
            <strong>${escapeHtml(payment.sourceAccount)}</strong>
        </div>
        <div class="detail-item">
            <span>Destination Account</span>
            <strong>${escapeHtml(payment.destinationAccount)}</strong>
        </div>
        <div class="detail-item">
            <span>Reference</span>
            <strong>${escapeHtml(payment.reference || '-')}</strong>
        </div>
        <div class="detail-item">
            <span>Created At</span>
            <strong>${formatDate(payment.createdAt)}</strong>
        </div>
        <div class="detail-item">
            <span>Updated At</span>
            <strong>${formatDate(payment.updatedAt)}</strong>
        </div>
        <div class="detail-item">
            <span>Error Code</span>
            <strong>${payment.errorCode || '-'}</strong>
        </div>
        <div class="detail-item">
            <span>Error Message</span>
            <strong>${escapeHtml(payment.errorMessage || '-')}</strong>
        </div>
    `;
}

function renderActions(payment) {
    actionButtons.innerHTML = '';
    actionButtons.classList.remove('hidden');

    if (payment.status === 'CREATED') {
        actionButtons.appendChild(actionButton('Validate', 'success-btn', () => changeStatus(payment.id, 'validate')));
        actionButtons.appendChild(actionButton('Fail', 'danger-btn', () => failPayment(payment.id)));
        return;
    }

    if (payment.status === 'VALIDATED') {
        actionButtons.appendChild(actionButton('Send', 'warning-btn', () => changeStatus(payment.id, 'send')));
        actionButtons.appendChild(actionButton('Fail', 'danger-btn', () => failPayment(payment.id)));
        return;
    }

    if (payment.status === 'SENT') {
        actionButtons.appendChild(actionButton('Complete', 'success-btn', () => changeStatus(payment.id, 'complete')));
        actionButtons.appendChild(actionButton('Fail', 'danger-btn', () => failPayment(payment.id)));
        return;
    }

    actionButtons.innerHTML = '<span class="hint">No actions available for this final status.</span>';
}

function actionButton(text, className, onClick) {
    const button = document.createElement('button');
    button.textContent = text;
    button.type = 'button';
    button.className = className;
    button.addEventListener('click', onClick);
    return button;
}

async function changeStatus(id, action) {
    try {
        await fetchJson(`${API_BASE}/${id}/${action}`, {
            method: 'POST'
        });
        await loadPayments();
        await loadPaymentDetails(id);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function failPayment(id) {
    const errorCode = prompt('Error code:', 'NETWORK_ERROR');
    if (!errorCode) {
        return;
    }

    const errorMessage = prompt('Error message:', 'Manually failed during demo') || 'Manually failed during demo';

    try {
        await fetchJson(`${API_BASE}/${id}/fail`, {
            method: 'POST',
            body: JSON.stringify({
                errorCode,
                errorMessage
            })
        });
        await loadPayments();
        await loadPaymentDetails(id);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderHistory(history) {
    historyTimeline.classList.remove('empty');
    historyTimeline.innerHTML = '';

    if (!history.length) {
        historyTimeline.classList.add('empty');
        historyTimeline.textContent = 'No history yet.';
        return;
    }

    for (const item of history) {
        const div = document.createElement('div');
        div.className = 'timeline-item';
        div.innerHTML = `
            <strong>${item.previousStatus || 'START'} → ${item.newStatus}</strong>
            <p>${escapeHtml(item.reason || '-')}</p>
            <p>Triggered by: ${escapeHtml(item.triggeredBy || '-')}</p>
            <p>${formatDate(item.changedAt)}</p>
        `;
        historyTimeline.appendChild(div);
    }
}

function openSidebar() {
    detailSidebar.classList.add('open');
}

function closeSidebar() {
    detailSidebar.classList.remove('open');
    selectedPaymentId = null;
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        },
        ...options
    });

    const contentType = response.headers.get('content-type');
    const body = contentType && contentType.includes('application/json')
        ? await response.json()
        : null;

    if (!response.ok) {
        if (body) {
            const validation = body.validationErrors && body.validationErrors.length
                ? ` Details: ${body.validationErrors.join('; ')}`
                : '';
            throw new Error(`${body.errorCode}: ${body.message}${validation}`);
        }

        throw new Error(`HTTP error ${response.status}`);
    }

    return body;
}

function statusBadge(status) {
    const cssClass = `status-${status.toLowerCase()}`;
    return `<span class="status-badge ${cssClass}">${status}</span>`;
}

function formatDate(value) {
    if (!value) {
        return '-';
    }
    return new Date(value).toLocaleString();
}

function showMessage(message, type) {
    formMessage.textContent = message;
    formMessage.className = `message ${type}`;
}

function hideMessage() {
    formMessage.textContent = '';
    formMessage.className = 'message hidden';
}

function fillDemoData() {
    const timestamp = Date.now();

    document.getElementById('idempotencyKey').value = `PAY-DEMO-${timestamp}`;
    document.getElementById('sourceAccount').value = 'ACC-001';
    document.getElementById('destinationAccount').value = 'ACC-USD-001';
    document.getElementById('amount').value = '10.00';
    document.getElementById('currency').value = 'USD';
    document.getElementById('reference').value = `Cross-currency demo payment ${timestamp}`;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
