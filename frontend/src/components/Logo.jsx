import logo from '../assets/logo_banco_acad_mico_png.png'

export function Logo({ className = '' }) {
  // Preserva o PNG original; no tema escuro, o CSS exibe a marca em branco para dar contraste.
  return <span className={`relative block aspect-[1046/300] max-w-full overflow-hidden ${className}`}>
    <img
      src={logo}
      alt="Banco Acadêmico"
      width="1600"
      height="480"
      className="absolute block h-auto max-w-none object-contain dark:brightness-0 dark:invert"
      style={{ width: `${1600 / 1046 * 100}%`, left: `${-26 / 1046 * 100}%`, top: `${-58 / 300 * 100}%` }}
    />
  </span>
}
