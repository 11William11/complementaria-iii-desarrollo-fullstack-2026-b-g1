// Problema 1: comportamiento con JavaScript
const boton = document.getElementById('boton');
const mensaje = document.getElementById('mensaje');

boton.addEventListener('click', () => {
  mensaje.textContent = 'Hiciste clic en el botón.';
});


// Problema 2: consumo de API con fetch (GET) y manejo de estados
const btnCargar = document.getElementById('btn-cargar');
const estadoCarga = document.getElementById('estado-carga');
const estadoError = document.getElementById('estado-error');
const listaUsuarios = document.getElementById('lista-usuarios');

const API_URL = 'https://jsonplaceholder.typicode.com/users';

async function cargarUsuarios() {
  estadoCarga.hidden = false;
  estadoError.hidden = true;
  listaUsuarios.innerHTML = '';

  try {
    const respuesta = await fetch(API_URL, { method: 'GET' });

    if (!respuesta.ok) {
      throw new Error(`Error HTTP: ${respuesta.status}`);
    }

    const usuarios = await respuesta.json();

    usuarios.forEach((usuario) => {
      const item = document.createElement('li');
      item.textContent = usuario.name;
      listaUsuarios.appendChild(item);
    });

  } catch (error) {
    estadoError.hidden = false;
    estadoError.textContent = 'No se pudieron cargar los datos.';
  } finally {
    estadoCarga.hidden = true;
  }
}

btnCargar.addEventListener('click', cargarUsuarios);

// Para crear un usuario se usaría el método POST.
// Para borrar un usuario se usaría el método DELETE.
