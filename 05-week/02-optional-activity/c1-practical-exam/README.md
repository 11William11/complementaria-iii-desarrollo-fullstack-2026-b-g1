# Problema 1 — Fundamentos web

- **HTML**: define la estructura y el contenido (encabezado, lista, botón).
- **CSS**: define la presentación visual (colores, espaciado, estilos).
- **JavaScript**: define el comportamiento (reacciona al clic del botón y cambia el contenido de la página).

# Problema 2 — Consumo de API

Se consume `https://jsonplaceholder.typicode.com/users` con `fetch` usando el método **GET**. Se manejan tres estados: carga (mientras se espera la respuesta), datos (cuando la lista se llena) y error (si la petición falla).

- Para **crear** un recurso se usaría el método **POST**.
- Para **borrar** un recurso se usaría el método **DELETE**.

# Problema 3 — Framework y SPA

- **Componente**: pieza reutilizable de interfaz que combina estructura, lógica y estilo propio.
- **Estado**: datos internos de un componente que, al cambiar, hacen que la interfaz se vuelva a renderizar.
- **Enrutamiento**: mecanismo que asocia una URL con una vista o componente, permitiendo navegar sin recargar la página.

Pseudocódigo mínimo:

```jsx
function ListaUsuarios() {
  const [usuarios, setUsuarios] = useState([]);

  useEffect(() => {
    fetch("/api/usuarios")
      .then(res => res.json())
      .then(data => setUsuarios(data));
  }, []);

  return (
    <ul>
      {usuarios.map(u => <li key={u.id}>{u.nombre}</li>)}
    </ul>
  );
}
```

Una SPA necesita una API porque, al cargar una sola vez el HTML y actualizar el contenido dinámicamente con JavaScript, requiere otra forma de obtener y enviar datos sin recargar la página; esa función la cumple la API.

## English requirement

A Single Page Application (SPA) loads one HTML page and updates its content dynamically with JavaScript, without requesting a new page from the server. A Multi-Page Application (MPA) loads a full new page from the server on every navigation, which makes it simpler but less fluid than a SPA.
