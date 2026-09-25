import { http } from './http'
import { tableauLigneSchema, type TableauLigne } from './schemas'
import { z } from 'zod'

export async function getTableau(promotionId: number): Promise<TableauLigne[]> {
  const { data } = await http.get('/tableau', { params: { promotionId } })
  return z.array(tableauLigneSchema).parse(data)
}
