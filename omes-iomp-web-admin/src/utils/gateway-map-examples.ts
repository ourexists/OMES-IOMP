export function mapExampleHtml(protocol?: string): string {
  const p = (protocol || '').toLowerCase().replace(/[\s_-]/g, '')
  if (p.includes('wincc')) {
    return `<div><strong>WINCC</strong><br/>属性：Machine1.Temperature<br/>运行：Process.RunStatus</div>`
  }
  return `<div><strong>REST / MQTT</strong><br/>属性：JSON 键如 temperature、data.status<br/>运行：run、status</div>`
}

export function supportsControlProtocol(_protocol?: string): boolean {
  return false
}
