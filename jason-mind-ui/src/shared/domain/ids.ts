/** O número de um id do Mind API (`i52669` vira `52669`), como o protótipo mostra no chip. */
export function numericId(id: string): string {
  return id.replace(/^\D+/, '')
}
