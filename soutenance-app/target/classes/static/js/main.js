// ENSAH Soutenance - Main JS

// Auto-dismiss alerts after 5s
document.querySelectorAll('.alert').forEach(el => {
    setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity 0.4s'; setTimeout(() => el.remove(), 400); }, 5000);
});

// Upload zone drag & drop
const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
if (dropZone && fileInput) {
    dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.style.borderColor = '#1a56db'; });
    dropZone.addEventListener('dragleave', () => { dropZone.style.borderColor = ''; });
    dropZone.addEventListener('drop', e => {
        e.preventDefault();
        dropZone.style.borderColor = '';
        const file = e.dataTransfer.files[0];
        if (file) {
            const dt = new DataTransfer();
            dt.items.add(file);
            fileInput.files = dt.files;
            document.getElementById('fileName').textContent = '📄 ' + file.name;
        }
    });
}

// Animate charge bars on load
document.querySelectorAll('.charge-bar').forEach(bar => {
    const w = bar.style.width;
    bar.style.width = '0';
    setTimeout(() => { bar.style.width = w; }, 100);
});
