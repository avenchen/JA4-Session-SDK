/**
 * JA4 Session Frontend SDK
 *
 * ?????????????? JA4 Header????? API ????
 * ?? ESM ?????????
 */
export class Ja4SessionClient {
    constructor(options = {}) {
        this.baseUrl = trimTrailingSlash(options.baseUrl || window.location.origin);
        this.ja4HeaderName = options.ja4HeaderName || 'X-JA4-Fingerprint';
        this.sessionStorageKey = options.sessionStorageKey || 'ja4-demo-persistent-id-hash';
        this.cachedFingerprint = null;
        this.cachedSignals = null;
        this.cachedJa4Header = null;
    }

    async collectFingerprint() {
        const signals = await this.#buildSignals();
        const canonical = Object.keys(signals)
            .sort()
            .map(key => `${key}=${signals[key]}`)
            .join('|');
        const fingerprintHash = await sha256(canonical);
        this.cachedSignals = signals;
        this.cachedFingerprint = fingerprintHash;
        if (!this.cachedJa4Header) {
            this.cachedJa4Header = `sim-${fingerprintHash.slice(0, 48)}`;
        }
        return {
            hash: fingerprintHash,
            signals,
            canonical
        };
    }

    getFingerprint() {
        if (!this.cachedFingerprint) {
            throw new Error('Fingerprint not collected yet. ?? collectFingerprint() ?????');
        }
        return {
            hash: this.cachedFingerprint,
            signals: this.cachedSignals
        };
    }

    setJa4Header(value) {
        this.cachedJa4Header = value;
    }

    getJa4Header() {
        if (!this.cachedJa4Header) {
            throw new Error('JA4 header ?????????? collectFingerprint()?');
        }
        return this.cachedJa4Header;
    }

    async login(username, password, overrides = {}) {
        await this.#ensureFingerprint();
        const payload = {
            username,
            password,
            clientFingerprint: this.cachedFingerprint,
            clientSignals: normalizeSignals(this.cachedSignals),
            ...overrides
        };
        return this.#request('/api/login', {
            method: 'POST',
            body: payload
        });
    }

    async getProfile() {
        return this.#request('/api/profile');
    }

    async logout() {
        return this.#request('/api/logout', { method: 'POST' });
    }

    async requestWithFingerprint(path, init = {}) {
        await this.#ensureFingerprint();
        return this.#request(path, init);
    }

    async #ensureFingerprint() {
        if (!this.cachedFingerprint) {
            await this.collectFingerprint();
        }
    }

    async #request(path, init = {}) {
        await this.#ensureFingerprint();
        const headers = new Headers(init.headers || {});
        headers.set('Accept', 'application/json, text/plain, */*');
        headers.set('Content-Type', 'application/json');
        headers.set(this.ja4HeaderName, this.cachedJa4Header);
        const response = await fetch(`${this.baseUrl}${path}`, {
            method: init.method || 'GET',
            credentials: 'include',
            headers,
            body: init.body ? JSON.stringify(init.body) : undefined
        });
        const text = await response.text();
        let parsed;
        try {
            parsed = JSON.parse(text);
        } catch (err) {
            parsed = text;
        }
        if (!response.ok) {
            const error = new Error(`JA4 request failed: ${response.status}`);
            error.status = response.status;
            error.response = parsed;
            throw error;
        }
        return parsed;
    }

    async #buildSignals() {
        const persistentId = await this.#getPersistentId();
        return {
            userAgent: navigator.userAgent,
            language: navigator.language,
            languages: (navigator.languages || []).join(','),
            platform: navigator.platform,
            hardwareConcurrency: navigator.hardwareConcurrency || 'unknown',
            deviceMemory: navigator.deviceMemory || 'unknown',
            cookieEnabled: navigator.cookieEnabled,
            doNotTrack: navigator.doNotTrack,
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'unknown',
            timezoneOffset: new Date().getTimezoneOffset(),
            screenResolution: `${window.screen.width}x${window.screen.height}`,
            screenAvailable: `${window.screen.availWidth}x${window.screen.availHeight}`,
            colorDepth: window.screen.colorDepth,
            pixelRatio: window.devicePixelRatio,
            touchPoints: navigator.maxTouchPoints || 0,
            sessionStorage: Boolean(safeSessionStorage()),
            localStorage: typeof localStorage !== 'undefined',
            persistentId,
            canvasFingerprint: getCanvasFingerprint(),
            webglFingerprint: getWebglFingerprint(),
            plugins: navigator.plugins ? Array.from(navigator.plugins).map(p => p.name).join('|') : 'unavailable',
            mimeTypes: navigator.mimeTypes ? Array.from(navigator.mimeTypes).map(m => m.type).join('|') : 'unavailable',
            referrer: document.referrer || '',
            pageUrl: location.href
        };
    }

    async #getPersistentId() {
        const store = safeSessionStorage();
        if (store) {
            const existing = store.getItem(this.sessionStorageKey);
            if (existing) {
                return existing;
            }
        }
        const rawId = crypto.randomUUID();
        const salt = `${navigator.userAgent}|${Intl.DateTimeFormat().resolvedOptions().timeZone || 'unknown'}`;
        const hash = await sha256(`${rawId}|${salt}`);
        if (store) {
            store.setItem(this.sessionStorageKey, hash);
        }
        return hash;
    }
}

function safeSessionStorage() {
    try {
        if (!window.sessionStorage) {
            return null;
        }
        sessionStorage.setItem('__ja4_test__', '1');
        sessionStorage.removeItem('__ja4_test__');
        return sessionStorage;
    } catch (err) {
        return null;
    }
}

function getCanvasFingerprint() {
    const canvas = document.createElement('canvas');
    canvas.width = 240;
    canvas.height = 60;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
        return 'unsupported';
    }
    ctx.textBaseline = 'top';
    ctx.font = '14px "Segoe UI"';
    ctx.fillStyle = '#1765ad';
    ctx.fillRect(0, 0, 240, 30);
    ctx.fillStyle = '#ffffff';
    ctx.fillText(navigator.userAgent, 2, 2);
    ctx.strokeStyle = '#ff6600';
    const uaFactor = Array.from(navigator.userAgent).reduce((sum, ch) => sum + ch.charCodeAt(0), 0);
    const controlY = 30 + (uaFactor % 20);
    ctx.beginPath();
    ctx.moveTo(0, 40);
    ctx.quadraticCurveTo(120, controlY, 240, 40);
    ctx.stroke();
    return canvas.toDataURL();
}

function getWebglFingerprint() {
    try {
        const canvas = document.createElement('canvas');
        const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
        if (!gl) {
            return 'unsupported';
        }
        const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
        if (!debugInfo) {
            return 'unavailable';
        }
        const vendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);
        const renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
        return `${vendor}__${renderer}`;
    } catch (err) {
        return `error:${err.message}`;
    }
}

function normalizeSignals(signals) {
    return Object.fromEntries(Object.entries(signals || {}).map(([key, value]) => [key, String(value)]));
}

async function sha256(input) {
    const encoder = new TextEncoder();
    const data = encoder.encode(input);
    const hash = await crypto.subtle.digest('SHA-256', data);
    const bytes = Array.from(new Uint8Array(hash));
    return bytes.map(b => b.toString(16).padStart(2, '0')).join('');
}

function trimTrailingSlash(url) {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}
