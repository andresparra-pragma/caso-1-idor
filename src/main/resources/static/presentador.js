/*
 * Barra de presentador para la demo en vivo (no es parte del portal):
 *  - Backend: Vulnerable / Seguro
 *  - "Ver petición": la última llamada a la API, con el NIT de la URL contra el del header
 *  - "Auditoría": los últimos accesos denegados que registró el backend seguro
 */
(function () {
    const CLAVE = 'demo.modoB';
    let modo = 'vulnerable';
    try { if (localStorage.getItem(CLAVE) === 'seguro') modo = 'seguro'; } catch (e) { /* sin almacenamiento */ }
    let ultima = null;
    let alCambiar = () => {};
    let panelActivo = null;

    function el(tag, clase, ...hijos) {
        const n = document.createElement(tag);
        if (clase) n.className = clase;
        hijos.flat().forEach(h => n.append(h instanceof Node ? h : String(h)));
        return n;
    }
    function cookie(nombre) {
        const m = document.cookie.match('(?:^|; )' + nombre + '=([^;]*)');
        return m ? decodeURIComponent(m[1]) : '';
    }

    // ---------- Barra ----------
    const barra = el('div', 'demo-barra');
    const seg = el('div', 'demo-seg');
    const botones = {};
    ['vulnerable', 'seguro'].forEach(m => {
        const b = el('button', m, m === 'vulnerable' ? 'Vulnerable' : 'Seguro');
        b.type = 'button';
        b.addEventListener('click', () => {
            modo = m;
            try { localStorage.setItem(CLAVE, m); } catch (e) { /* ignorar */ }
            pintarModo(); alCambiar();
        });
        botones[m] = b; seg.append(b);
    });
    function pintarModo() { Object.entries(botones).forEach(([m, b]) => b.classList.toggle('activo', m === modo)); }

    const btnPeticion = el('button', 'demo-btn', 'Ver petición'); btnPeticion.type = 'button';
    const btnAuditoria = el('button', 'demo-btn', 'Auditoría'); btnAuditoria.type = 'button';
    barra.append(el('span', 'etq', 'Backend'), seg, btnPeticion, btnAuditoria);

    const panel = el('div', 'demo-panel');
    function abrir(cual) {
        panelActivo = panelActivo === cual ? null : cual;
        panel.classList.toggle('visible', !!panelActivo);
        btnPeticion.classList.toggle('activo', panelActivo === 'peticion');
        btnAuditoria.classList.toggle('activo', panelActivo === 'auditoria');
        refrescar();
    }
    btnPeticion.addEventListener('click', () => abrir('peticion'));
    btnAuditoria.addEventListener('click', () => abrir('auditoria'));
    document.addEventListener('keydown', e => { if (e.key === 'Escape' && panelActivo) abrir(panelActivo); });

    function refrescar() {
        if (panelActivo === 'peticion') pintarPeticion();
        else if (panelActivo === 'auditoria') pintarAuditoria();
    }

    // ---------- Panel: última petición ----------
    function pintarPeticion() {
        panel.replaceChildren();
        panel.append(el('div', 'cab', el('b', null, 'Última petición a la API'), el('span', null, 'Backend ' + modo)));
        if (!ultima) { panel.append(el('div', 'nota', 'Todavía no hay peticiones.')); return; }

        const daneHeader = (ultima.header.match(/\d+/) || [''])[0];
        const mDane = ultima.url.match(/\/colegios\/([^/]+)\/cursos/);
        const mCurso = ultima.url.match(/\/cursos\/([^/]+)\/estudiantes/);
        const daneUrl = mDane ? decodeURIComponent(mDane[1]) : null;
        const colegioRespuesta = ultima.data && ultima.data.colegio ? ultima.data.colegio : null;
        const ajeno = (daneUrl && daneUrl !== daneHeader) || (colegioRespuesta && colegioRespuesta.dane !== daneHeader);

        const pre = el('pre');
        const linea1 = el('span');
        linea1.append(el('span', 'metodo', ultima.metodo + ' '));
        if (daneUrl) {
            const [antes, despues] = ultima.url.split('/colegios/' + mDane[1]);
            linea1.append(antes + '/colegios/', el('span', daneUrl !== daneHeader ? 'hl-malo' : 'hl-ok', daneUrl), despues);
        } else if (mCurso) {
            const [antes, despues] = ultima.url.split('/cursos/' + mCurso[1]);
            linea1.append(antes + '/cursos/', el('span', colegioRespuesta && colegioRespuesta.dane !== daneHeader ? 'hl-malo' : 'hl-ok', decodeURIComponent(mCurso[1])), despues);
        } else linea1.append(ultima.url);
        const linea2 = el('span'); linea2.append(el('span', 'k', 'X-Colegio: '), el('span', 'hl-ok', ultima.header));
        const ok = ultima.status >= 200 && ultima.status < 300;
        const linea3 = el('span'); linea3.append(el('span', 'k', '→ '), el('span', ok && ajeno ? 'st-malo' : (ok ? 'st-ok' : 'st-malo'),
            ultima.status + (ultima.status === 200 ? ' OK' : ultima.status === 403 ? ' Forbidden' : ultima.status === 404 ? ' Not Found' : '')));
        pre.append(linea1, '\n', linea2, '\n', linea3);
        panel.append(pre);

        const nota = el('div', 'nota');
        if (ok && ajeno) {
            const quien = colegioRespuesta ? colegioRespuesta.nombre : 'otro colegio';
            nota.append('El API Gateway validó el header (tu colegio). El backend usó el dato de la URL y entregó información de ',
                el('strong', null, quien), quien.endsWith('.') ? '' : '.');
        } else if (ultima.status === 403) {
            nota.append('El backend tomó el colegio del header validado, comparó contra el recurso pedido y respondió el mismo 403 genérico.');
        } else if (ok) {
            nota.append('El recurso pertenece al colegio de la sesión.');
        }
        panel.append(nota);
    }

    // ---------- Panel: auditoría ----------
    async function pintarAuditoria() {
        panel.replaceChildren();
        panel.append(el('div', 'cab', el('b', null, 'Auditoría del backend · accesos denegados'), el('span', null, 'log del servidor')));
        let eventos = [], disponible = true;
        try {
            eventos = await (await fetch('/api/demo/auditoria', { cache: 'no-store' })).json();
            disponible = (await (await fetch('/api/demo/matriculas', { cache: 'no-store' })).json()).disponible;
        } catch (e) { /* sin servidor */ }
        const pre = el('pre', 'log');
        if (!eventos.length) pre.append('(sin accesos denegados todavía)');
        eventos.slice(0, 6).forEach((e, i) => {
            if (i) pre.append('\n');
            pre.append(e.hora + ' ', el('span', 'w', 'WARN AUDITORIA'), ' usuario=' + e.usuario + ' op=' + e.operacion +
                ' colegio=' + e.colegioAutorizado + ' recurso="' + e.recurso + '" motivo="' + e.motivo + '"');
        });
        panel.append(pre);

        const toggle = el('label', 'toggle');
        const chk = document.createElement('input'); chk.type = 'checkbox'; chk.checked = !disponible;
        chk.addEventListener('change', async () => {
            await fetch('/api/demo/matriculas?disponible=' + !chk.checked,
                { method: 'POST', headers: { 'X-XSRF-TOKEN': cookie('XSRF-TOKEN') } });
        });
        toggle.append(chk, 'Simular caída del servicio de matrículas (fail closed)');
        panel.append(toggle);
    }

    document.body.append(panel, barra);
    pintarModo();

    window.Demo = {
        modo: () => modo,
        registrarPeticion: p => { ultima = p; if (panelActivo === 'peticion') pintarPeticion(); if (panelActivo === 'auditoria') pintarAuditoria(); },
        alCambiarModo: fn => { alCambiar = fn; },
    };
})();
