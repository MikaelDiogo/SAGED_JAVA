-- Replace is_active boolean with status enum on telegram_demand_requesters
ALTER TABLE saged.telegram_demand_requesters
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE saged.telegram_demand_requesters
    SET status = CASE WHEN is_active THEN 'ACTIVE' ELSE 'INACTIVE' END;

ALTER TABLE saged.telegram_demand_requesters
    DROP COLUMN is_active;

-- Allow null department_id for PENDING self-registrations from the bot
ALTER TABLE saged.telegram_demand_requesters
    ALTER COLUMN department_id DROP NOT NULL;

-- Add leadership column to org_units
ALTER TABLE saged.org_units
    ADD COLUMN leadership VARCHAR(128);

-- Replace the 5 placeholder seeds with the full 22-secretaria structure
DELETE FROM saged.org_units;

INSERT INTO saged.org_units (id, code, name, leadership, updated_at, source) VALUES
    ('00000000-0000-0000-0000-000000000010', '1',     'Gabinete da(o) Prefeita(o)',                          'Chefe de Gabinete',                    NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000011', '01.01', 'Gabinete do(a) Vice-Prefeito(a)',                     'Chefe de Gabinete',                    NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000020', '2',     'Secretaria Municipal de Governo',                    'Secretário(a) de Governo',             NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000030', '3',     'Procuradoria Geral do Município',                    'Procurador(a) Geral',                  NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000040', '4',     'Controladoria Geral do Município',                   'Controlador(a) Geral',                 NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000050', '5',     'Sec. de Administração, Finanças e Orçamento',        'Secretário(a) de Admin/Finanças',      NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000001', '6',     'Sec. de Planejamento e Tecnologia da Informação',    'Secretário(a) de Planejamento e TI',   NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000002', '7',     'Secretaria Municipal de Educação',                   'Secretário(a) de Educação',            NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000003', '8',     'Secretaria Municipal de Saúde',                      'Secretário(a) de Saúde',               NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000004', '9',     'Secretaria Municipal de Assistência Social',         'Secretário(a) de Assistência Social',  NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000100', '10',    'Sec. de Comunicação Social e Relações Públicas',     'Secretário(a) de Comunicação',         NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000110', '11',    'Secretaria Municipal de Segurança Cidadã e Trânsito','Secretário(a) de Segurança',           NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000120', '12',    'Secretaria Municipal de Cultura',                    'Secretário(a) de Cultura',             NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000130', '13',    'Sec. de Proteção à Mulher e à Família',              'Secretário(a) de Proteção à Mulher',   NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000140', '14',    'Secretaria Municipal de Esporte e Lazer',            'Secretário(a) de Esporte e Lazer',     NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000150', '15',    'Sec. de Des. Econômico, Empreendedorismo e Trabalho','Secretário(a) de Des. Econômico',      NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000160', '16',    'Secretaria Municipal do Turismo',                    'Secretário(a) de Turismo',             NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000170', '17',    'Sec. de Desenvolvimento Agrário e Pecuário',         'Secretário(a) de Des. Agrário',        NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000180', '18',    'Sec. de Infância, Adolescência e Juventude',         'Secretário(a) de Infância/Juventude',  NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000190', '19',    'Sec. de Recursos Hídricos e Defesa Civil',           'Secretário(a) de Recursos Hídricos',   NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000005', '20',    'Sec. de Infraestrutura e Serviços Públicos',         'Secretário(a) de Infraestrutura',      NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000210', '21',    'Secretaria Municipal de Meio Ambiente',              'Secretário(a) de Meio Ambiente',       NOW(), 'seed'),
    ('00000000-0000-0000-0000-000000000220', '22',    'Secretaria Municipal de Desenvolvimento Regional',   'Secretário(a) de Des. Regional',       NOW(), 'seed');
