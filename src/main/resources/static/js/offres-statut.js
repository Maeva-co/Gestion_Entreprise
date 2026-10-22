/**
 * Changement de statut d'une offre depuis la liste (modal + fetch, sans rechargement).
 * Fichier : src/main/resources/static/js/offres-statut.js
 */
(function () {
  "use strict";

  var LIBELLES = {
    BROUILLON: "Brouillon",
    PUBLIEE: "Publiée",
    SUSPENDUE: "Suspendue",
    CLOTUREE: "Clôturée",
  };
  var CLASSES_BADGE = ["badge-success", "badge-warning", "badge-neutral"];

  var dialog = document.getElementById("statutModal");
  if (!dialog) return;

  var form = document.getElementById("statutForm");
  var select = document.getElementById("statutSelect");
  var titre = document.getElementById("statutOffreTitre");
  var courant = document.getElementById("statutCourant");
  var note = document.getElementById("statutNote");
  var erreur = document.getElementById("statutErreur");
  var bouton = document.getElementById("statutConfirmer");
  var carte = document.getElementById("offresCard");

  var offreId = null;

  function classeBadge(code) {
    if (code === "PUBLIEE") return "badge-success";
    if (code === "BROUILLON") return "badge-warning";
    return "badge-neutral";
  }

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
    dialog.showModal();
  }

  function appliquer(data) {
    var ligne = document.querySelector('tr[data-offre-id="' + offreId + '"]');

    // Compteurs des filtres (valeurs du serveur).
    Object.keys(data.compteurs).forEach(function (code) {
      document
        .querySelectorAll('[data-compteur="' + code + '"]')
        .forEach(function (el) {
          el.textContent = data.compteurs[code];
        });
    });

    if (!ligne) return;

    // Filtre actif : l'offre n'appartient plus à cette liste.
    var actif = carte ? carte.dataset.statutActif : "";
    if (actif && actif !== data.statut) {
      var corps = ligne.parentNode;
      ligne.remove();
      if (!corps.querySelector("tr")) window.location.reload();
      return;
    }

    var badge = ligne.querySelector('[data-role="badge"]');
    CLASSES_BADGE.forEach(function (c) {
      badge.classList.remove(c);
    });
    badge.classList.add(classeBadge(data.statut));
    ligne.querySelector('[data-role="statut-libelle"]').textContent =
      data.libelle;
    ligne.querySelector('[data-role="date-publication"]').textContent =
      data.datePublication || "—";

    var btn = ligne.querySelector(".js-statut-btn");
    btn.dataset.statut = data.statut;
    btn.dataset.transitions = data.transitions.join(",");
    btn.hidden = data.transitions.length === 0;
  }

  document.addEventListener("click", function (e) {
    var btn = e.target.closest(".js-statut-btn");
    if (btn) ouvrir(btn);
    if (e.target.closest("[data-close-statut]")) dialog.close();
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
        dialog.close();
      })
      .catch(function () {
        erreur.textContent =
          "Erreur de communication avec le serveur. Réessayez.";
        bouton.disabled = false;
      });
  });
})();