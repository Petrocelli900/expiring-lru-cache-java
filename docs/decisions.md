# Decisões técnicas

## Política LRU e expiração

LinkedHashMap em ordem de acesso mantém o item menos recentemente utilizado no início. Leitura válida promove o item; escrita substitui o valor, renova o TTL e o promove. No instante exato da expiração, o item já não é válido. Ler não renova TTL.

Antes de inserir, todos os itens expirados são removidos, evitando expulsar itens válidos enquanto expirados ocupam espaço. size retorna somente itens válidos. get remove apenas o item consultado quando expirado.

## Tempo e concorrência

Clock é injetável e os testes avançam o tempo sem sleep. Operações são sincronizadas em um monitor, garantindo capacidade e ordem consistentes dentro da instância. Valores nulos e capacidade/TTL inválidos são rejeitados.

## Compromissos

Purga em put e size custa O(n); get custa O(1) amortizado. Clock.systemUTC usa tempo civil: mudanças do relógio podem antecipar ou atrasar a expiração. Um adaptador de tempo monotônico seria uma extensão para uso mais exigente.

Sem carregamento automático, métricas, persistência, cache distribuído ou garantias de imutabilidade dos valores. Retornar um objeto mutável permite ao chamador modificá-lo fora do lock. Esta versão é uma biblioteca didática, não substitui caches de produção consolidados.

