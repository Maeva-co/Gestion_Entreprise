/**
 * Changement de statut d'une offre depuis la liste (modal + fetch, sans rechargement).
 * Fichier : src/main/resources/static/js/offres-statut.js
 *
 * Le modal reprend le design du modal de refus : un .modal-backdrop affiché ou
 * masqué avec la classe "hidden".
 */
(function () {
  "use strict";

  var LIBELLES = {
    BROUILLON: "Brouillon",
    PUBLIEE: "Publiée",
    SUSPENDUE: "Suspendue",
    CLOTUREE: "Clôturée",
  };

  var modal = document.getElementById("statutModal");
  if (!modal) return;

  var form = document.getElementById("statutForm");
  var select = document.getElementById("statutSelect");
  var titre = document.getElementById("statutOffreTitre");
  var courant = document.getElementById("statutCourant");
  var note = document.getElementById("statutNote");
  var erreur = document.getElementById("statutErreur");
  var bouton = document.getElementById("statutConfirmer");
  var conteneur = document.getElementById("offresCard");

  var offreId = null;

  function entetes() {
    var h = {
      Accept: "application/json",
      "Content-Type": "application/x-www-form-urlencoded",
    };
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) h[header.content] = token.content;
    return h;
  }

  function majNote() {
    note.hidden = select.value !== "CLOTUREE";
  }

  function ouvrir(btn) {
    offreId = btn.dataset.offreId;
    titre.textContent = btn.dataset.titre;
    courant.textContent = LIBELLES[btn.dataset.statut] || btn.dataset.statut;

    select.innerHTML = "";
    (btn.dataset.transitions || "")
      .split(",")
      .filter(Boolean)
      .forEach(function (code) {
        var opt = document.createElement("option");
        opt.value = code;
        opt.textContent = LIBELLES[code] || code;
        select.appendChild(opt);
      });

    erreur.textContent = "";
    bouton.disabled = false;
    majNote();
    modal.classList.remove("hidden");
    select.focus();
  }

  function fermer() {
    modal.classList.add("hidden");
  }

  function appliquer(data) {
    // Compteurs des filtres (valeurs du serveur).
    Object.keys(data.compteurs).forEach(function (code) {
      document
        .querySelectorAll('[data-compteur="' + code + '"]')
        .forEach(function (el) {
          el.textContent = data.compteurs[code];
        });
    });

    var carte = document.querySelector(
      'article.offre-card[data-offre-id="' + offreId + '"]'
    );
    if (!carte) return;

    // Filtre actif : l'offre n'appartient plus à cette liste.
    var actif = conteneur ? conteneur.dataset.statutActif : "";
    if (actif && actif !== data.statut) {
      var grille = carte.parentNode;
      carte.remove();
      if (!grille.querySelector(".offre-card")) window.location.reload();
      return;
    }

    carte.querySelector('[data-role="badge"]').className =
      "offre-pill pill-" + data.statut;
    carte.querySelector('[data-role="statut-libelle"]').textContent =
      data.libelle;
    carte.querySelector('[data-role="date-publication"]').textContent =
      data.datePublication || "—";

    var btn = carte.querySelector(".js-statut-btn");
    btn.dataset.statut = data.statut;
    btn.dataset.transitions = data.transitions.join(",");
    btn.hidden = data.transitions.length === 0;

    // Les candidatures n'existent qu'une fois l'offre publiée.
    var lien = carte.querySelector(".js-candidatures-link");
    if (lien) lien.hidden = data.statut === "BROUILLON";
  }

  document.addEventListener("click", function (e) {
    var btn = e.target.closest(".js-statut-btn");
    if (btn) ouvrir(btn);
    if (e.target.closest("[data-close-statut]")) fermer();
  });

  // Clic sur le fond sombre ou touche Échap : fermeture.
  modal.addEventListener("click", function (e) {
    if (e.target === modal) fermer();
  });
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && !modal.classList.contains("hidden")) fermer();
  });

  select.addEventListener("change", majNote);

  form.addEventListener("submit", function (e) {
    e.preventDefault();
    erreur.textContent = "";
    bouton.disabled = true;

    var url = form.dataset.urlBase + "/" + offreId + "/statut";

    fetch(url, {
      method: "POST",
      headers: entetes(),
      credentials: "same-origin",
      body: new URLSearchParams({ statut: select.value }),
    })
      .then(function (r) {
        return r.json().then(function (data) {
          return { ok: r.ok, data: data };
        });
      })
      .then(function (res) {
        if (!res.ok) {
          erreur.textContent = res.data.message || "Modification impossible.";
          bouton.disabled = false;
          return;
        }
        appliquer(res.data);
        fermer();
      })
      .catch(function () {
        erreur.textContent =
          "Erreur de communication avec le serveur. Réessayez.";
        bouton.disabled = false;
      });
  });
})();