# Uploads: validação e operação

## Comportamento

- Materiais: PDF, DOC, DOCX, PPT, PPTX, JPG/JPEG, PNG e GIF, com conteúdo real coerente com a extensão. O MIME declarado pelo cliente não é fonte de confiança. Arquivos vazios são recusados.
- Limite por material: 20 MiB. A requisição multipart dispõe de 21 MiB, incluindo JSON e delimitadores.
- PDF: abertura estrutural com PDFBox, 1 a 2000 páginas, sem senha, scripts, ações automáticas ou anexos. Estruturas excessivamente complexas são recusadas.
- Office: abertura estrutural com Apache POI; pacotes OOXML têm limite de 2000 entradas e 80 MiB descompactados. Macros, ActiveX, objetos incorporados e referências externas ativas são recusados. Hiperlinks comuns continuam permitidos.
- Imagens dos materiais: formato real e decodificação verificados, até 16 megapixels somados e 100 quadros. Fotos de perfil preservam as regras de 2 MiB, JPEG/PNG e regravação em PNG até 512 pixels.
- Nomes originais são limitados a 200 caracteres e perdem componentes de caminho; caracteres de controle são recusados. O nome físico é gerado pelo servidor.
- `responsabilidadeConfirmada: true` é obrigatório no JSON de criação. A declaração é exigida pela API, mas não cria histórico de aceite.
- Materiais antigos continuam acessíveis conforme as mesmas regras de moderação, exclusivamente para download. Novos arquivos recebem o prefixo físico `validado-v1-`, usado para reconhecer a política de validação aplicada, sem migration. Esse marcador pressupõe diretório e banco protegidos contra escrita por terceiros.
- A prévia PDF usa PDF.js para pintar páginas em canvas, sem ativar scripts, formulários, links ou anexos. Recursos de fontes e decodificadores são distribuídos com o frontend, sem CDN. Miniaturas carregam ao se aproximar da área visível. O download permanece disponível se a prévia não puder ser gerada.
- Gravação temporária na mesma raiz, movimentação atômica e compensação de rollback. Falhas durante a publicação removem o temporário. A fila de exclusão existente continua repetindo falhas de remoção.
- Raiz, caminhos de leitura e exclusão recusam redirecionamentos por links simbólicos/junctions. A pasta deve ser gravável somente pela conta de execução: essas checagens não substituem permissões do sistema operacional.

## Limites padrão configuráveis

| Variável | Padrão | Abrangência |
| --- | --- | --- |
| `UPLOAD_ROOT` | `Uploads` | Raiz de armazenamento |
| `UPLOAD_USER_QUOTA_BYTES` | 524288000 | 500 MiB de materiais por autor, incluindo pendentes e rejeitados |
| `UPLOAD_DISK_RESERVE_BYTES` | 104857600 | Reserva de 100 MiB no disco antes de gravar |
| `UPLOAD_CONCURRENCY` | 2 | Processamentos de upload simultâneos por instância |
| `UPLOAD_ATTEMPTS_15MIN` | 20 | Tentativas por usuário por 15 minutos, por instância |
| `UPLOAD_BYTES_15MIN` | 104857600 | 100 MiB recebidos por usuário por 15 minutos, por instância |

Materiais e fotos compartilham os limites de frequência e concorrência. Há um processamento simultâneo por usuário. Respostas de limite usam HTTP 429. A cota acumulada dos materiais usa o banco sob bloqueio da linha do autor, inclusive entre instâncias; arquivos legados sem tamanho registrado consomem conservadoramente 20 MiB cada. Frequência e volume por janela são mantidos em memória e reiniciam com o processo. Em múltiplas instâncias, aplicar também limites compartilhados no gateway ou serviço de admissão. O limite multipart e a proteção de tráfego no proxy devem ser mantidos: a admissão na aplicação ocorre depois do processamento multipart.

Ao alterar `UPLOAD_ROOT`, os caminhos já persistidos e os arquivos precisam de uma migração operacional planejada; mudar somente a variável não transfere os materiais antigos. Não foi feita migração de arquivos nem modificação de dados existentes.

## Antimalware

A integração usa o protocolo ClamD `INSTREAM`, com limite de tempo total, leitura limitada de resposta e bloqueio em caso de resposta incompleta, erro ou indisponibilidade. O conteúdo é enviado por socket, sem expor caminhos locais. Materiais são verificados no envio e novamente antes da aprovação. Fotos são verificadas após o reprocessamento, antes de persistir o PNG.

O scanner fica **desabilitado por padrão** para permitir execução local sem ClamAV. Há aviso no log quando desabilitado. Para ativar:

1. Disponibilizar ClamAV atualizado. O Compose oferece o perfil opcional: `docker compose --profile antimalware up -d clamav`.
2. Aguardar o carregamento/atualização das assinaturas e a disponibilidade do ClamD.
3. Configurar `UPLOAD_ANTIMALWARE_ENABLED=true`, `CLAMAV_HOST=localhost`, `CLAMAV_PORT=3310` e, se necessário, `CLAMAV_TIMEOUT_MS=15000`, no ambiente do backend; reiniciar o backend.
4. Manter `StreamMaxLength` do scanner suficiente para materiais de 20 MiB. A porta do Compose está vinculada somente a `127.0.0.1`; se backend e scanner estiverem em contêineres separados, usar uma rede privada e o hostname do serviço.

Com o scanner habilitado, falhas de verificação retornam HTTP 503, sem persistir o novo upload nem aprovar o material. Uma detecção retorna HTTP 400. Não há serviço externo de scanner em execução criado por esta implementação; os testes do protocolo usam servidor local simulado. Varredura não garante ausência de todas as ameaças nem faz revisão retroativa dos materiais já aprovados.

O perfil Compose é opcional e sua imagem `stable` deve ser atualizada de forma controlada. Consulte os requisitos de recursos e atualização de assinaturas na [documentação oficial do ClamAV](https://docs.clamav.net/manual/Installing/Docker.html) e a especificação do [protocolo ClamD](https://docs.clamav.net/manual/Usage/ClamdProtocol.html).

## Verificação

Os testes de segurança cobrem conteúdo disfarçado, vazios, excesso de tamanho, nomes, PDF ativo, Office válido, macros, expansão de ZIP, limite de admissão, cota, responsabilidade, gravação com falha e respostas do ClamD. A cobertura existente de moderação, autenticação do arquivo, fotos, rollback e exclusão foi preservada e usa PDFs válidos. O teste de links simbólicos pode ser ignorado quando o Windows não permite criá-los.

Uma interrupção abrupta do processo pode deixar temporários ou arquivos sem registro antes do commit. A compensação cobre falhas normais e rollback; conciliação após crash deve comparar arquivos e banco durante manutenção, sem excluir arquivos em processamento.
