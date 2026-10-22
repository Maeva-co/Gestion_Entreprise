function openRefuserModal(btn) {
  const modal = document.getElementById('refuser-modal');
  const form = document.getElementById('refuser-form');

  // L'URL dépend uniquement de l'id de la ligne, pas du rôle :
  // le serveur choisit l'étape (RH ou DG) selon l'utilisateur connecté.
  form.action = form.dataset.base + btn.dataset.ligneId + '/refuser';

  form.reset();
  modal.classList.remove('hidden');
  document.getElementById('refuser-commentaire').focus();
}

function closeRefuserModal() {
  document.getElementById('refuser-modal').classList.add('hidden');
}

// Fermeture avec Échap et au clic sur le fond sombre
document.addEventListener('keydown', function (e) {
  if (e.key === 'Escape') closeRefuserModal();
});

document.addEventListener('click', function (e) {
  const modal = document.getElementById('refuser-modal');
  if (modal && e.target === modal) closeRefuserModal();
});