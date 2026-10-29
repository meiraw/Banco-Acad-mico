import logo from '../assets/banco-academico-logo.png'

export function Logo({ className = '' }) {
  return <img
    src={logo}
    alt="Banco Acadêmico"
    width="983"
    height="416"
    className={`block h-auto max-w-full object-contain mix-blend-multiply dark:invert dark:mix-blend-screen ${className}`}
  />
}
