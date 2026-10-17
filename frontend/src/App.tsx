import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import {
  ArrowDownRight,
  ArrowRight,
  Bell,
  ChartNoAxesColumnIncreasing,
  ChevronDown,
  CircleHelp,
  Clock3,
  FileText,
  FolderOpen,
  Image,
  LayoutDashboard,
  LockKeyhole,
  LogOut,
  Search,
  ShieldCheck,
  Upload,
  UsersRound,
} from 'lucide-react'

type Role = 'ADMIN' | 'EDITOR' | 'LECTOR'
type AuthStatus = { authenticated: boolean; googleConfigured: boolean; email?: string; role?: Role }
type DashboardData = { title: string; subtitle: string; demoData: boolean; metrics: { label: string; value: string; note: string }[] }
type DriveImage = { id: string; name: string; contentType: string; visibleTo: Role[] }

const roleContent: Record<Role, {
  title: string
  subtitle: string
  metrics: { label: string; value: string; note: string; icon: typeof UsersRound }[]
  sectionTitle: string
  sectionNote: string
}> = {
  ADMIN: {
    title: 'Buenos días, equipo.',
    subtitle: 'Aquí está el pulso de tu espacio institucional.',
    metrics: [
      { label: 'Personas activas', value: '248', note: '+12 este mes', icon: UsersRound },
      { label: 'Publicaciones', value: '36', note: '4 por revisar', icon: FileText },
      { label: 'Recursos en Drive', value: '82', note: 'Sincronización pendiente', icon: Image },
    ],
    sectionTitle: 'Actividad del portal',
    sectionNote: 'Resumen general de contenido y accesos',
  },
  EDITOR: {
    title: 'Hola, equipo editorial.',
    subtitle: 'Continúa con las publicaciones que necesitan tu atención.',
    metrics: [
      { label: 'En edición', value: '08', note: '3 actualizadas hoy', icon: FileText },
      { label: 'Por publicar', value: '04', note: 'Próxima: 14:30', icon: Clock3 },
      { label: 'Imágenes', value: '21', note: 'En tu biblioteca', icon: Image },
    ],
    sectionTitle: 'Tu espacio editorial',
    sectionNote: 'Borradores recientes y tareas asignadas',
  },
  LECTOR: {
    title: 'Bienvenida, comunidad.',
    subtitle: 'Información y recursos destacados para ti.',
    metrics: [
      { label: 'Novedades', value: '12', note: 'Desde tu última visita', icon: Bell },
      { label: 'Recursos', value: '28', note: 'Disponibles para ti', icon: FolderOpen },
      { label: 'Informes', value: '06', note: 'Publicados este trimestre', icon: ChartNoAxesColumnIncreasing },
    ],
    sectionTitle: 'Información destacada',
    sectionNote: 'Contenido compartido con tu perfil',
  },
}

const navigation = [
  { label: 'Inicio', icon: LayoutDashboard },
  { label: 'Publicaciones', icon: FileText },
  { label: 'Biblioteca', icon: FolderOpen },
  { label: 'Informes', icon: ChartNoAxesColumnIncreasing },
]

function App() {
  const [role, setRole] = useState<Role>('ADMIN')
  const [activePage, setActivePage] = useState('Inicio')
  const [session, setSession] = useState<AuthStatus | null>(null)
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  const [driveImages, setDriveImages] = useState<DriveImage[]>([])
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const imageInput = useRef<HTMLInputElement>(null)
  const content = roleContent[role]
  const apiBase = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')
  const apiUrl = (path: string) => `${apiBase}${path}`

  useEffect(() => {
    let active = true
    fetch(apiUrl('/api/auth/status'), { credentials: 'include' })
      .then((response) => response.ok ? response.json() as Promise<AuthStatus> : Promise.reject())
      .then((status) => {
        if (!active) return
        setSession(status)
        if (!status.authenticated || !status.role) return
        setRole(status.role)
        return Promise.all([
          fetch(apiUrl('/api/dashboard'), { credentials: 'include' }).then((response) => response.ok ? response.json() as Promise<DashboardData> : null),
          fetch(apiUrl('/api/images'), { credentials: 'include' }).then((response) => response.ok ? response.json() as Promise<DriveImage[]> : []),
        ]).then(([summary, images]) => {
          if (!active) return
          if (summary) setDashboard(summary)
          setDriveImages(images)
        })
      })
      .catch(() => {
        if (active) setSession(null)
      })
    return () => { active = false }
  }, [])

  const isAuthenticated = session?.authenticated === true
  const metrics = dashboard?.metrics.map((metric, index) => ({ ...metric, icon: content.metrics[index]?.icon ?? FolderOpen })) ?? content.metrics
  const pageTitle = isAuthenticated && dashboard ? dashboard.title : content.title
  const pageSubtitle = isAuthenticated && dashboard ? dashboard.subtitle : content.subtitle

  async function uploadImage(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    setUploading(true)
    setUploadError('')
    try {
      const csrfResponse = await fetch(apiUrl('/api/csrf'), { credentials: 'include' })
      if (!csrfResponse.ok) throw new Error('No se pudo preparar la protección de sesión.')
      const { token } = await csrfResponse.json() as { token: string }
      const formData = new FormData()
      formData.append('file', file)
      formData.append('visibleTo', (role === 'ADMIN' ? ['ADMIN', 'EDITOR', 'LECTOR'] : ['EDITOR', 'LECTOR']).join(','))
      const response = await fetch(apiUrl('/api/images'), {
        method: 'POST',
        credentials: 'include',
        headers: { 'X-XSRF-TOKEN': token },
        body: formData,
      })
      if (!response.ok) throw new Error('No se pudo guardar la imagen en Google Drive.')
      const uploaded = await response.json() as DriveImage
      setDriveImages((images) => [uploaded, ...images])
    } catch (error) {
      setUploadError(error instanceof Error ? error.message : 'Error al cargar la imagen.')
    } finally {
      setUploading(false)
      event.target.value = ''
    }
  }

  async function logout() {
    const csrfResponse = await fetch(apiUrl('/api/csrf'), { credentials: 'include' })
    if (!csrfResponse.ok) return
    const { token } = await csrfResponse.json() as { token: string }
    await fetch(apiUrl('/logout'), { method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': token } })
    window.location.assign(import.meta.env.BASE_URL)
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#inicio" aria-label="Portal Institucional, inicio">
          <span className="brand-mark">P<span>.</span></span>
          <span className="brand-name">portal<span>institucional</span></span>
        </a>
        <div className="workspace-label">ESPACIO DE TRABAJO</div>
        <nav className="main-nav" aria-label="Navegación principal">
          {navigation.map(({ label, icon: Icon }) => (
            <button
              className={`nav-item ${activePage === label ? 'active' : ''}`}
              key={label}
              onClick={() => setActivePage(label)}
              type="button"
            >
              <Icon size={18} strokeWidth={1.8} />
              <span>{label}</span>
              {label === 'Publicaciones' && role !== 'LECTOR' && <span className="nav-count">4</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="access-note">
            <span className="access-icon"><ShieldCheck size={17} /></span>
            <div><strong>Acceso protegido</strong><span>Permisos por perfil</span></div>
          </div>
          <button className="help-link" type="button"><CircleHelp size={17} /> Centro de ayuda</button>
          <div className="profile-row">
            <div className="avatar">IR</div>
            <div className="profile-name"><strong>{isAuthenticated ? session.email : 'Invitada de prueba'}</strong><span>{isAuthenticated ? role : `${role} · DEMO`}</span></div>
            <ChevronDown size={16} />
          </div>
        </div>
      </aside>

      <main className="main-area" id="inicio">
        <header className="topbar">
          <div className="breadcrumbs"><span>Portal</span><span className="crumb-slash">/</span><strong>{activePage}</strong></div>
          <div className="top-actions">
            <label className="search-box">
              <Search size={16} />
              <input aria-label="Buscar" placeholder="Buscar en el portal" />
              <kbd>⌘ K</kbd>
            </label>
            <button className="icon-button notification-button" aria-label="Notificaciones" type="button"><Bell size={18} /><i /></button>
            {isAuthenticated ? (
              <><span className="session-badge"><ShieldCheck size={15} /> {role}</span><button className="icon-button logout-button" onClick={logout} type="button" aria-label="Cerrar sesión" title="Cerrar sesión"><LogOut size={17} /></button></>
            ) : session?.googleConfigured ? (
              <a className="google-login" href={apiUrl('/oauth2/authorization/google')}><LockKeyhole size={15} /> Iniciar sesión</a>
            ) : (
              <button className="google-login" type="button" disabled title="Configura Google OAuth y activa el perfil google del backend"><LockKeyhole size={15} /> Google no conectado</button>
            )}
          </div>
        </header>

        <div className="page-content">
          {(!isAuthenticated || dashboard?.demoData) && <div className="preview-banner">
            <span className="preview-dot" />
            <span><strong>{isAuthenticated ? 'Indicadores de demostración' : 'Vista previa'}</strong> · los datos no son operativos</span>
            {!isAuthenticated && <div className="role-switch" aria-label="Vista previa por rol">
              {(['ADMIN', 'EDITOR', 'LECTOR'] as Role[]).map((item) => (
                <button key={item} type="button" aria-pressed={role === item} onClick={() => setRole(item)}>{item}</button>
              ))}
            </div>}
          </div>}

          <section className="welcome-row">
            <div>
              <p className="eyebrow">SÁBADO, 17 DE OCTUBRE DE 2026 <span>·</span> RESUMEN</p>
              <h1>{activePage === 'Inicio' ? pageTitle : activePage}</h1>
              <p className="welcome-subtitle">{activePage === 'Inicio' ? pageSubtitle : `Contenido disponible para el perfil ${role.toLowerCase()}.`}</p>
            </div>
            {isAuthenticated ? (
              <a className="report-button" href={apiUrl('/api/reports/summary.pdf')}><FileText size={17} /> Descargar informe <ArrowDownRight size={15} /></a>
            ) : (
              <button className="report-button" type="button" disabled title="Inicia sesión para descargar un informe"><FileText size={17} /> Descargar informe <ArrowDownRight size={15} /></button>
            )}
          </section>

          <section className="metric-grid" aria-label="Indicadores principales">
            {metrics.map(({ label, value, note, icon: Icon }, index) => (
              <article className={`metric metric-${index + 1}`} key={label}>
                <div className="metric-top"><span>{label}</span><Icon size={18} strokeWidth={1.8} /></div>
                <div className="metric-value">{value}</div>
                <div className="metric-note"><span className="metric-trend">↗</span>{note}</div>
              </article>
            ))}
          </section>

          <section className="content-grid">
            <article className="activity-panel">
              <div className="section-heading">
                  <div><h2>{dashboard?.title ?? content.sectionTitle}</h2><p>{dashboard?.subtitle ?? content.sectionNote}</p></div>
                <button className="text-button" type="button" onClick={() => setActivePage('Publicaciones')}>Ver todo <ArrowRight size={15} /></button>
              </div>
              {role === 'LECTOR' ? (
                <div className="feature-story">
                  <div className="story-art"><span>COMUNIDAD</span><div>Un espacio<br />para avanzar.</div><div className="art-lines" /></div>
                  <div className="story-copy"><span className="tag tag-blue">NOVEDAD</span><h3>Lo que nos mueve, nos conecta</h3><p>Conoce las iniciativas y recursos que nuestra comunidad comparte este mes.</p><a href="#biblioteca">Leer publicación <ArrowRight size={14} /></a></div>
                </div>
              ) : (
                <div className="activity-list">
                  <div className="activity-item"><span className="activity-marker marker-green" /><div className="activity-copy"><strong>{role === 'ADMIN' ? 'Informe de gestión trimestral' : 'Guía de bienvenida actualizada'}</strong><span>{role === 'ADMIN' ? 'María Torres · Informe' : 'Ana Ruiz · Borrador'}</span></div><span className="activity-status status-review">{role === 'ADMIN' ? 'Revisar' : 'En edición'}</span><time>Hace 24 min</time></div>
                  <div className="activity-item"><span className="activity-marker marker-blue" /><div className="activity-copy"><strong>{role === 'ADMIN' ? 'Nueva colección de recursos' : 'Agenda de actividades de octubre'}</strong><span>{role === 'ADMIN' ? 'Biblioteca · 8 elementos' : 'Comunidad · Publicación'}</span></div><span className="activity-status status-published">Publicado</span><time>Hace 2 h</time></div>
                  <div className="activity-item"><span className="activity-marker marker-yellow" /><div className="activity-copy"><strong>{role === 'ADMIN' ? 'Política de participación' : 'Convocatoria de propuestas'}</strong><span>{role === 'ADMIN' ? 'Actualizado por Laura Pérez' : 'Equipo editorial · Revisión'}</span></div><span className="activity-status status-draft">Borrador</span><time>Ayer</time></div>
                </div>
              )}
              <div className="panel-footer"><span>Última actualización: hoy, 09:42</span><span className="live-indicator"><i /> Actualizado</span></div>
            </article>

            <aside className="right-rail">
              <article className="drive-panel">
                <div className="drive-head"><span className="drive-icon"><Image size={18} /></span><span className="drive-label">BIBLIOTECA VISUAL</span>
                  {isAuthenticated && role !== 'LECTOR' && <label className="drive-upload" title={uploading ? 'Subiendo imagen' : 'Subir imagen a Google Drive'}>
                    <Upload size={15} />
                    <input ref={imageInput} type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadImage} disabled={uploading} aria-label="Subir imagen" />
                  </label>}
                  <button className="dots-button" aria-label="Más opciones" type="button">···</button></div>
                <h2>Imágenes del equipo</h2>
                <p>Recursos compartidos desde Google Drive.</p>
                <div className="image-preview-grid" aria-label="Imágenes visibles para el perfil">
                  {isAuthenticated ? driveImages.slice(0, 3).map((image) => (
                    <a className="drive-image" key={image.id} href={apiUrl(`/api/images/${encodeURIComponent(image.id)}/content`)} target="_blank" rel="noreferrer" title={image.name}>
                      <img src={apiUrl(`/api/images/${encodeURIComponent(image.id)}/content`)} alt={image.name} />
                    </a>
                  )) : <>
                    <div className="image-preview image-one"><span>01</span></div>
                    <div className="image-preview image-two"><span>02</span></div>
                    <div className="image-preview image-three"><span>03</span></div>
                  </>}
                  {isAuthenticated && driveImages.length === 0 && <span className="drive-empty">No hay imágenes autorizadas</span>}
                </div>
                {uploadError && <p className="upload-error" role="alert">{uploadError}</p>}
                <div className="drive-foot"><span><FolderOpen size={14} /> {isAuthenticated ? `${driveImages.length} recursos visibles` : 'Vistas de muestra'}</span><button type="button" title="La conexión Drive requiere una sesión Google"><ArrowRight size={16} /></button></div>
              </article>
              <article className="quick-report">
                <span className="report-icon"><ChartNoAxesColumnIncreasing size={18} /></span>
                <div><strong>Informes disponibles</strong><span>Datos según tu perfil</span></div>
                {isAuthenticated ? <a href={apiUrl('/api/reports/summary.pdf')} aria-label="Descargar informe PDF"><ArrowRight size={16} /></a> : <button type="button" disabled aria-label="Inicia sesión para descargar el informe"><ArrowRight size={16} /></button>}
              </article>
            </aside>
          </section>
          <footer className="page-footer"><span>Portal Institucional <span className="footer-sep">/</span> Entorno de demostración</span><span>Los permisos de acceso se validan en el servidor</span></footer>
        </div>
      </main>
    </div>
  )
}

export default App
