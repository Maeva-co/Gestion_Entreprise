(() => {
  'use strict';

  const TYPE_REMPLACEMENT = 1;
  const TYPE_CREATION = 2;

  const form = document.getElementById('demande-form');
  if (!form) return;

  const baseUrl = form.dataset.baseUrl;
  const selDep = document.getElementById('departement');
  const selType = document.getElementById('typeDemande');
  const zoneLignes = document.getElementById('zone-lignes');
  const conteneur = document.getElementById('lignes');
  const tpl = document.getElementById('ligne-template');
  const btnAjouter = document.getElementById('btn-ajouter');
  const totalEl = document.getElementById('budget-total');
  const alertBox = document.getElementById('alert');

  const fmt = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  let meres = [];      // postes mères du département sélectionné
  let jetonDep = 0;    // ignore les réponses obsolètes si le département change vite

  const depId = () => parseInt(selDep.value, 10) || null;
  const typeId = () => parseInt(selType.value, 10) || null;
  const estRemplacement = () => typeId() === TYPE_REMPLACEMENT;
  const lignes = () => Array.from(conteneur.querySelectorAll('[data-ligne]'));
  const q = (ligne, role) => ligne.querySelector(`[data-role="${role}"]`);

  // ---------- Utilitaires ----------

  function afficherMessage(texte, type = 'error') {
    alertBox.textContent = texte;
    alertBox.className = 'mb-4 rounded-md px-4 py-3 text-sm ' +
      (type === 'error' ? 'bg-red-50 text-red-700 border border-red-200'
                        : 'bg-green-50 text-green-700 border border-green-200');
  }

  function masquerMessage() {
    alertBox.className = 'hidden';
    alertBox.textContent = '';
  }

  async function getJson(chemin, params) {
    const url = new URL(baseUrl + chemin, window.location.origin);
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
    const res = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Erreur serveur.');
    }
    return res.json();
  }

  // ---------- Budget ----------

  function majBudgetLigne(ligne) {
    const unitaire = parseFloat(ligne.dataset.unit || '0');
    const effectif = parseInt(q(ligne, 'effectif').value, 10) || 0;
    q(ligne, 'budget').value = unitaire ? fmt.format(unitaire * effectif) : '';
    majTotal();
  }

  function majTotal() {
    const total = lignes().reduce((somme, l) => {
      const unitaire = parseFloat(l.dataset.unit || '0');
      const effectif = parseInt(q(l, 'effectif').value, 10) || 0;
      return somme + unitaire * effectif;
    }, 0);
    totalEl.textContent = fmt.format(total);
  }

  // ---------- Gestion des lignes ----------

  function reinitialiserLigne(ligne) {
    delete ligne.dataset.unit;
    delete ligne.dataset.prefixe;
    delete ligne.dataset.indice;

    const fille = q(ligne, 'fille-select');
    fille.innerHTML = '<option value="">— Choisir —</option>';
    q(ligne, 'fille-auto').value = '';

    const effectif = q(ligne, 'effectif');
    effectif.value = '';
    effectif.removeAttribute('max');
    effectif.disabled = true;
    q(ligne, 'effectif-hint').textContent = '';
    q(ligne, 'budget').value = '';
    majTotal();
  }

  function ajouterLigne() {
    const noeud = tpl.content.firstElementChild.cloneNode(true);
    const remplacement = estRemplacement();
    q(noeud, 'fille-select').classList.toggle('hidden', !remplacement);
    q(noeud, 'fille-auto').classList.toggle('hidden', remplacement);
    const selMere = q(noeud, 'mere');
    selMere.innerHTML = '<option value="">— Choisir —</option>';
    meres.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.id;
      opt.textContent = m.titre;
      selMere.appendChild(opt);
    });
    conteneur.appendChild(noeud);
    reinitialiserLigne(noeud);
    majBoutonsSuppression();
  }

  function majBoutonsSuppression() {
    const seule = lignes().length === 1;
    lignes().forEach(l => {
      const btn = q(l, 'supprimer');
      btn.disabled = seule;
      btn.classList.toggle('opacity-40', seule);
      btn.classList.toggle('cursor-not-allowed', seule);
    });
  }

  function reinitialiserFormulaireLignes() {
    conteneur.innerHTML = '';
    const pret = depId() && typeId();
    zoneLignes.classList.toggle('hidden', !pret);
    if (pret) ajouterLigne();
    majTotal();
  }

  // ---------- Type 1 : remplacement ----------

  function remplirVacants(ligne, vacants) {
    const select = q(ligne, 'fille-select');
    select.innerHTML = '';

    if (!vacants.length) {
      select.innerHTML = '<option value="">Aucun poste vacant dans ce département</option>';
      return;
    }
    select.insertAdjacentHTML('beforeend', '<option value="">— Choisir —</option>');
    vacants.forEach(v => {
      const opt = document.createElement('option');
      opt.value = v.id;
      opt.textContent = v.titre;
      opt.dataset.max = v.effectifMax;
      select.appendChild(opt);
    });

    if (vacants.length === 1) {
      select.value = vacants[0].id;
      onFilleChange(ligne);
    }
  }

  function onFilleChange(ligne) {
    const select = q(ligne, 'fille-select');
    const option = select.options[select.selectedIndex];
    const max = parseInt(option?.dataset.max, 10) || 0;
    const effectif = q(ligne, 'effectif');

    effectif.disabled = !max;
    if (max) {
      effectif.max = max;
      effectif.value = Math.min(parseInt(effectif.value, 10) || 1, max);
      q(ligne, 'effectif-hint').textContent = `Maximum : ${max}`;
    } else {
      effectif.value = '';
      effectif.removeAttribute('max');
      q(ligne, 'effectif-hint').textContent = '';
    }
    majBudgetLigne(ligne);
  }

  // ---------- Type 2 : création ----------

  /** Numérote DEV 4, DEV 5... quand plusieurs lignes ont la même mère. */
  function rafraichirTitresCreation() {
    const vus = {};
    lignes().forEach(l => {
      const mere = q(l, 'mere').value;
      if (!mere || !l.dataset.prefixe) {
        q(l, 'fille-auto').value = '';
        return;
      }
      vus[mere] = (vus[mere] || 0) + 1;
      q(l, 'fille-auto').value = `${l.dataset.prefixe} ${parseInt(l.dataset.indice, 10) + vus[mere] - 1}`;
    });
  }

  // ---------- Changement de poste mère ----------

  async function onMereChange(ligne) {
    const mereId = q(ligne, 'mere').value;
    reinitialiserLigne(ligne);
    rafraichirTitresCreation();
    if (!mereId || !depId()) return;

    const jeton = (ligne._jeton = (ligne._jeton || 0) + 1); // ignore les réponses obsolètes
    try {
      if (estRemplacement()) {
        const [vacants, budget] = await Promise.all([
          getJson('/api/postes-vacants', { departementId: depId(), posteMereId: mereId }),
          getJson('/api/budget-mere', { posteMereId: mereId })
        ]);
        if (jeton !== ligne._jeton) return;
        ligne.dataset.unit = budget.salaireUnitaire;
        remplirVacants(ligne, vacants);
      } else {
        const p = await getJson('/api/prochain-poste', { departementId: depId(), posteMereId: mereId });
        if (jeton !== ligne._jeton) return;
        ligne.dataset.prefixe = p.titreMere;
        ligne.dataset.indice = p.prochainIndice;
        ligne.dataset.unit = p.budget.salaireUnitaire;
        const effectif = q(ligne, 'effectif');
        effectif.disabled = false;
        effectif.value = 1;
        rafraichirTitresCreation();
      }
      majBudgetLigne(ligne);
    } catch (e) {
      afficherMessage(e.message);
    }
  }

  // ---------- Événements ----------

  selDep.addEventListener('change', async () => {
    const jeton = ++jetonDep;
    masquerMessage();
    meres = [];
    conteneur.innerHTML = '';
    zoneLignes.classList.add('hidden');

    if (depId()) {
      try {
        const reponse = await getJson('/api/postes-meres', { departementId: depId() });
        if (jeton !== jetonDep) return;
        meres = reponse;
        if (!meres.length) afficherMessage('Aucun poste mère défini dans ce département.');
      } catch (e) {
        afficherMessage(e.message);
      }
    }
    reinitialiserFormulaireLignes();
  });
  selType.addEventListener('change', reinitialiserFormulaireLignes);
  btnAjouter.addEventListener('click', () => {
    ajouterLigne();
    rafraichirTitresCreation();
  });

  conteneur.addEventListener('change', e => {
    const ligne = e.target.closest('[data-ligne]');
    if (!ligne) return;
    const role = e.target.dataset.role;
    if (role === 'mere') onMereChange(ligne);
    else if (role === 'fille-select') onFilleChange(ligne);
    else if (role === 'effectif') {
      if (!(parseInt(e.target.value, 10) >= 1) && !e.target.disabled) e.target.value = 1;
      majBudgetLigne(ligne);
    }
  });

  conteneur.addEventListener('input', e => {
    if (e.target.dataset.role !== 'effectif') return;
    const ligne = e.target.closest('[data-ligne]');
    const max = parseInt(e.target.max, 10);
    // Remplacement : l'effectif ne peut pas dépasser le nombre de postes vacants.
    if (max && parseInt(e.target.value, 10) > max) e.target.value = max;
    majBudgetLigne(ligne);
  });

  conteneur.addEventListener('click', e => {
    const btn = e.target.closest('[data-role="supprimer"]');
    if (!btn || btn.disabled) return;
    btn.closest('[data-ligne]').remove();
    majBoutonsSuppression();
    rafraichirTitresCreation();
    majTotal();
  });

  // ---------- Soumission ----------

  form.addEventListener('submit', async e => {
    e.preventDefault();
    masquerMessage();

    if (!depId() || !typeId()) {
      afficherMessage('Sélectionnez un département et un type de demande.');
      return;
    }

    const payload = { departementId: depId(), typeId: typeId(), lignes: [] };
    const cumul = {};

    for (const l of lignes()) {
      const mereId = parseInt(q(l, 'mere').value, 10) || null;
      const posteId = estRemplacement() ? (parseInt(q(l, 'fille-select').value, 10) || null) : null;
      const effectif = parseInt(q(l, 'effectif').value, 10) || 0;

      if (!mereId) return afficherMessage('Choisissez un poste mère sur chaque ligne.');
      if (estRemplacement() && !posteId) return afficherMessage('Choisissez le poste à remplacer sur chaque ligne.');
      if (effectif < 1) return afficherMessage("L'effectif doit être au moins égal à 1.");

      if (estRemplacement()) {
        const max = parseInt(q(l, 'fille-select').selectedOptions[0].dataset.max, 10) || 0;
        cumul[posteId] = (cumul[posteId] || 0) + effectif;
        if (cumul[posteId] > max) {
          return afficherMessage(`Effectif total demandé pour « ${q(l, 'fille-select').selectedOptions[0].textContent} » supérieur aux postes vacants (${max}).`);
        }
      }
      payload.lignes.push({ posteMereId: mereId, posteId, effectif });
    }

    const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

    const btnSubmit = form.querySelector('[type="submit"]');
    btnSubmit.disabled = true;
    try {
      const res = await fetch(baseUrl + '/api/creer', { method: 'POST', headers, body: JSON.stringify(payload) });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || 'Erreur lors de la création de la demande.');

      form.reset();
      reinitialiserFormulaireLignes();
      afficherMessage(`Demande n°${data.id} créée avec succès.`, 'success');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      afficherMessage(err.message);
    } finally {
      btnSubmit.disabled = false;
    }
  });
})();