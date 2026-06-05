async function api(path, options = {}) {
  const res = await fetch(path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  const text = await res.text();
  let data = null;
  if (text) try { data = JSON.parse(text); } catch { data = {}; }
  if (!res.ok) throw new Error(data?.error || `HTTP ${res.status}`);
  return data;
}

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('authForm');
  const errEl = document.getElementById('authError');
  const linkPanel = document.getElementById('linkPanel');
  const mode = document.body.dataset.authMode || 'login';

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    errEl.textContent = '';
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const username = document.getElementById('minecraftUsername')?.value?.trim();

    try {
      if (mode === 'register') {
        const data = await api('/api/web/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, password, minecraftUsername: username }),
        });
        if (data.verification?.devLink) {
          sessionStorage.setItem('verifyDevLink', data.verification.devLink);
        }
        window.location.href = 'account.html';
        return;
      }
      await api('/api/web/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      window.location.href = 'store.html';
    } catch (err) {
      const map = {
        email_exists: 'Email já registado.',
        minecraft_user_not_found: 'Username Minecraft não encontrado.',
        invalid_credentials: 'Email ou password incorrectos.',
        'password: mín. 8 chars, 1 maiúscula e 1 número': 'Password fraca: 8+ chars, 1 maiúscula e 1 número.',
      };
      errEl.textContent = map[err.message] || err.message;
    }
  });

  document.getElementById('linkBtn')?.addEventListener('click', async () => {
    const username = document.getElementById('linkUsername').value.trim();
    const out = document.getElementById('linkResult');
    try {
      const data = await api('/api/web/auth/link/start', {
        method: 'POST',
        body: JSON.stringify({ username }),
      });
      out.innerHTML = `<p class="success-msg">Código: <strong>${data.code}</strong></p>
        <p class="muted">No servidor: <code>/link ${data.code}</code></p>`;
      linkPanel?.classList.remove('hidden');
    } catch (err) {
      out.textContent = err.message;
    }
  });
});
