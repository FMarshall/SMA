//Bibliotecas JADE para multiagentes

import jade.core.Agent;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetResponder;
import jade.proto.SubscriptionInitiator;
import jade.proto.ContractNetInitiator;
import jade.proto.SubscriptionResponder;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;
//Bibliotecas JADE para comportamentos temporais
import jade.core.behaviours.Behaviour;
//import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.util.leap.ArrayList;

import java.io.File;
//Bibliotecas para lidar com arquivos XML
import java.io.IOException;
import java.lang.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
//import org.omg.CORBA.portable.UnknownException;

public class agenteAlimentador extends Agent {

	double cargaPerdida, cargaTotalDisponivelMicrorrede = 0;
	
	public void setup(){
		//Pega o nome do agente no Banco de Dados
		final String agenteAlimentador = getLocalName();
		final Element agenteALBD = carregaBD(agenteAlimentador);
	
		final MessageTemplate filtroAtuacao = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE),
		  		MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)); 
		
		//Filtro contract net para receber solicitações de outros ALs
		final MessageTemplate filtroContractNet = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );
		
		/**
		 * M�todo para receber os valores atuais de carga
		 */		
		/*
		addBehaviour(new TickerBehaviour(this,100) {
			public void onTick(){
				//ACLMessage filtroAtualizacao = receive();
				//if(filtroAtualizacao != null){
				
				ACLMessage filtroRotina = receive();
				if(filtroRotina != null){
				
					//exibirMensagem(agree);
					
					String agenteChave = filtroRotina.getSender().getLocalName();
					System.out.println("-<<"+agenteAlimentador+">>: recebi um valor de carga de "+agenteChave);
					String cargaDemandada = filtroRotina.getContent();
					agenteAlimentadorBD.getChild("chaves").getChild(agenteChave).setAttribute("carga",cargaDemandada); //Atualiza o seu banco de dados
				
				}
			}
		});
		*/
		/********************************************************************************************************************** 
		 *Parte do FIPA Subscribe participante para receeber informação de curto ou carga
		 **********************************************************************************************************************/
		addBehaviour(new SubscriptionResponder(this, filtroAtuacao) {
			private static final long serialVersionUID = 1L;

			protected ACLMessage handleSubscription(ACLMessage subscription){
				String conteudo = subscription.getContent();
				
//				if(conteudo.equals("curto")){
//					
//				}// Fim do if(conteudo.equals("curto"))
				
				exibirAviso(myAgent, "Fui informado de uma falta");
				ACLMessage resposta = subscription.createReply();
				resposta.setContent("ok");
				resposta.setPerformative(ACLMessage.AGREE); 
				
				//Atualizar  o XML para saber onde foi a falta
				String agenteChaveSobFalta = subscription.getSender().getLocalName();
				agenteALBD.getChild("chaves").getChild(agenteChaveSobFalta).setAttribute("atuacao","sim");
				
				String referencia = agenteChaveSobFalta.split("_")[1]; //500e3/Só para pegar o número do agente chave para avisar somente os ajusante
				exibirAviso(myAgent, "A referência do agente chave é "+referencia);
				
//				String referenciaDaChave = referencia.split("R")[1];
				int referenciaDaChaveAtuante =Integer.parseInt(referencia.split("R")[1]); //Só para pegar o número do agente chave para avisar somente os ajusante
				exibirAviso(myAgent, "A chave atuante é: "+referenciaDaChaveAtuante);
				
				//*********Saber se tem agente chave e quantos são
				List lista = agenteALBD.getChild("chaves").getChildren(); 
				Iterator i = lista.iterator();
				
				int cont = 0; //inicia cont com zero
				
			    while(i.hasNext()) { 
			    	Element elemento = (Element) i.next();
			    	String nome = String.valueOf(elemento.getName());
			    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
			    		cont = cont + 1; //Se houver algum agente chave, incrementa o contador
			    	}
			    }
//			    if(cont!=0){ // Se existirem agentes chave
			    if(cont>referenciaDaChaveAtuante){ //Se o número de agente chave for maior que o índice da chave atuante, é porque com certeza há chaves a jusante da chave atuante
					exibirAviso(myAgent, "Há chaves a jusante da que sentiu o curto.");
			    	/**********************************************************************************
				     * Protocolo FIPA Request para solicitar que todos os ACs abram suas chaves
				     * 
				     *********************************************************************************/
			  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			  		msg.setContent("abra");
			  		
			  		//Cria-se uma lista para percorrer a tag CHAVES (agentes chave)
			  		List lista1 = agenteALBD.getChild("chaves").getChildren(); 
					Iterator i1 = lista1.iterator();
										
				    while(i1.hasNext()) { 
				    	Element elemento = (Element) i1.next();
				    	String nome = String.valueOf(elemento.getName());
				    	
						exibirAviso(myAgent, "Analisando se aviso ao agente chave "+nome+" que comande a abertura de seu religador.");
						
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
				    		int referenciaDaChave = Integer.parseInt(nome.split("_")[1].split("R")[1]); //Analisa-se a posição da chave visada
				    		
				    		exibirAviso(myAgent, "A referência de "+nome+" é "+referenciaDaChave);
				    		
				    		if(referenciaDaChave>referenciaDaChaveAtuante){ //Se a chave analisada estiver localizada a jusante da chave atuante então
				    			exibirAviso(myAgent, "A referência de "+nome+" é maior que a da chave atuante que é "+referenciaDaChaveAtuante);
				    			
				    			if(elemento.getAttributeValue("atuacao").equals("nao")){ //Se o agente chave não é o que atuou então vou mandar uma mensagem pra ele
					    			
				    				exibirAviso(myAgent, "Solicitando ao agente chave "+nome+" que comande a abertura de sua chave.");
//				    				
						    		msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
						    		/*
						    		 * Aqui eu já sei a chave que abrirá e já pego é o valor de carga perdida dela no XML
						    		 * Na verdade, basta pegar o valor de corrente medida no trecho a jusante ao do trecho em falta. Ele já me dirá
						    		 * toda a carga perdida
						    		 */
						    		if(referenciaDaChave == referenciaDaChaveAtuante + 1){
						    			double cargaPerdida = Double.parseDouble(elemento.getAttributeValue("carga"));
//							    		cargaTotalPerdida = cargaTotalPerdida + cargaPerdida;
//						    			cargaTotalPerdida = cargaPerdida;
						    		}
						    		
//						    		cargaPerdida = 0; //Acho que nem precisa disso não
				    			}// Fim do if para saber se atuou ou não
				    		}// Fim do if para saber se referenciaDaChave>referenciaDaChaveAtuante
				    	}// Fim do if para saber se há chave
				    }// Fim do while(i.hasNext())	
			  		
			  		
				    addBehaviour(new AchieveREInitiator(myAgent, msg) {
						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
						
							
						}
						protected void handleRefuse(ACLMessage refuse) {
//							System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
//							nResponders--;
						}
						protected void handleFailure(ACLMessage failure) {
							if (failure.getSender().equals(myAgent.getAMS())) {
								// FAILURE notification from the JADE runtime: the receiver
								// does not exist
								System.out.println("Responder does not exist");
							}
							else {
								System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
							}
						}
						protected void handleAllResultNotifications(Vector notifications) {
//							if (notifications.size() < nResponders) {
//								// Some responder didn't reply within the specified timeout
//								System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
//							}
						}
					}); //Fim do addBehcaviour do Request para avisar os agentes chave que abram
			  		
				    
				  //*********Já sei que há trechos a jusante (consequentemente agentes chave, agora, saber se tem micrrodes nos trechos a jusante.
					exibirAviso(myAgent, "Saber se tem microrredes para serem avisadas e ilharem.");
				    List lista2 = agenteALBD.getChild("microrredes").getChildren(); //Vê todos agente chave para ver se no trechos deles tem microrrede
					Iterator i2 = lista2.iterator();
					
					int contMicrorredes = 0; //inicia cont com zero
					
				    while(i2.hasNext()) { 
				    	Element elemento = (Element) i2.next();
				    	String nome = String.valueOf(elemento.getName());
				    	
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
				    		exibirAviso(myAgent, "Vamos analisar se analiso a existência de microrredes no mesmo trecho do AC "+nome+". A microrrde tem que está no mesmo trech do curto ou depois");
				    		//Tenho que analisar o nome do agente chave para começar a avisar a microrredes a partir da que pertence ao mesmo trecho do AC  que atuou por curto
				    		int referenciaACAnalisado = Integer.parseInt(nome.split("_")[1].split("R")[1]);
				    		
				    		if(referenciaACAnalisado>=referenciaDaChaveAtuante){
				    			exibirAviso(myAgent, nome+" está a jusante do da chave que atuou ou é a chave que atuou. Vamos analisar se há microrrede no trecho do agente chave "+nome);
					    		//Estou na TAG nome do agente chave. Vou olhar se no trecho dele tem microrredes. 
					    		List lista3 = agenteALBD.getChild("microrredes").getChild(nome).getChildren(); //Vê todos agente chave para ver se no trechos deles tem microrrede
								Iterator i3 = lista3.iterator();
								
							    while(i3.hasNext()) { 
							    	Element elemento2 = (Element) i3.next();
							    	String nomeMicrorrede = String.valueOf(elemento2.getName());
							    	exibirAviso(myAgent, "Era pra ter aqui o nome nenhum ou a referência de um apc. No caso, tá dando: "+nomeMicrorrede);
							    	
							    	if (nomeMicrorrede!= null && nomeMicrorrede.length()>0 && nomeMicrorrede!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
							    		contMicrorredes = contMicrorredes + 1; //Se houver microrrede, incrementa o contador
							    	}// Fim do if para saber se tem microrredes de fato, (se o nome é diferente de nenhum)
							    }//Fim do while(i3.hasNext()) para percorrer as microrredes dentro da tag do agente chave
				    		}// Fim do if comparando a referências das chaves para avisar as microrredes do trecho afetado em diante
				    	}// Fim do if para saber se tem agente chave (se o nome é diferente de nenhum)
				    }//Fim do while(i3.hasNext()) para percorrer o nome das microrredes
				    
				    if(contMicrorredes>0){ //Se existirem microrredes para serem avisadas
				    	exibirAviso(myAgent, "Há microrredes para serem alertadas de modo que ilhem.");
				    	/**********************************************************************************
					     * Protocolo FIPA Request para solicitar que APCs abram suas chaves
					     * 
					     *********************************************************************************/
				  		ACLMessage msg2 = new ACLMessage(ACLMessage.REQUEST);
				  		msg2.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				  		msg2.setContent("abra");
				  		
				  		//Cria-se uma lista para percorrer a tag CHAVES (agentes chave)
				  		List lista3 = agenteALBD.getChild("microrredes").getChildren(); 
						Iterator i3 = lista3.iterator();
						
						while(i3.hasNext()) { 
					    	Element elemento = (Element) i3.next();
					    	String nome = String.valueOf(elemento.getName());
					    	
					    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
					    	
					    		//Tenho que analisar o nome do agente chave para começar a avisar a microrredes a partir da que pertence ao mesmo trecho do AC  que atuou por curto
					    		int referenciaACAnalisado = Integer.parseInt(nome.split("_")[1].split("R")[1]);
					    		
					    		if(referenciaACAnalisado>=referenciaDaChaveAtuante){
					    		
						    		//Estou na TAG nome do agente chave. Vou olhar se no trecho dele tem microrredes. 
						    		List lista4 = agenteALBD.getChild("microrredes").getChild(nome).getChildren(); //Vê todos agente chave para ver se no trechos deles tem microrrede
									Iterator i4 = lista4.iterator();
									
								    while(i4.hasNext()) { 
								    	Element elemento4 = (Element) i4.next();
								    	String nomeMicrorrede = String.valueOf(elemento4.getName());
								    	
								    	if (nomeMicrorrede!= null && nomeMicrorrede.length()>0 && nomeMicrorrede!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
								    		exibirAviso(myAgent, "Vou avisar a microrrde "+nomeMicrorrede+" que ilhe!!!!!");
								    		msg2.addReceiver(new AID((String) nomeMicrorrede, AID.ISLOCALNAME));
								    		
								    		if(referenciaACAnalisado>referenciaDaChaveAtuante){//Aqui é só para pegar o valor de potência disponível das microrredes a jusante do trecho em falta
								    			/*
									    		 * Aqui eu já sei a microrrede que está a jusante do trecho afetado e que pode disponibilizar carga
									    		 */
//									    		double cargaDisponivelMicrorrede = Double.parseDouble(elemento4.getAttributeValue("carga"));
								    			double cargaDisponivelMicrorrede = Double.parseDouble(agenteALBD.getChild("microrredes").getChild(nome).getChild(nomeMicrorrede).getAttributeValue("carga"));
									    		cargaTotalDisponivelMicrorrede = cargaTotalDisponivelMicrorrede + cargaDisponivelMicrorrede;
								    		}
								    	}// Fim do if para saber se tem microrredes de fato, (se o nome é diferente de nenhum)
								    }//Fim do while(i3.hasNext()) para percorrer as microrredes dentro da tag do agente chave
					    		}// Fim do if comparando a referências das chaves para avisar as microrredes do trecho afetado em diante
					    	}// Fim do if para saber se tem agente chave (se o nome é diferente de nenhum)
					    }//Fim do while(i3.hasNext()) para percorrer o nome das microrredes
				   
				  		
					    addBehaviour(new AchieveREInitiator(myAgent, msg2) {
							protected void handleInform(ACLMessage inform) {
								System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
							
								
							}
							protected void handleRefuse(ACLMessage refuse) {
//								System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
//								nResponders--;
							}
							protected void handleFailure(ACLMessage failure) {
								if (failure.getSender().equals(myAgent.getAMS())) {
									// FAILURE notification from the JADE runtime: the receiver
									// does not exist
									System.out.println("Responder does not exist");
								}
								else {
									System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
								}
							}
							protected void handleAllResultNotifications(Vector notifications) {
//								if (notifications.size() < nResponders) {
//									// Some responder didn't reply within the specified timeout
//									System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
//								}
							}
						}); //Fim do addBehcaviour do Request para avisar os agentes chave que abram
				    }
				    
				    /*Aqui eu vou calcular o valor de potencia perdida consultando o XML. 
				     * Se preciso, coloco aquele comportamento que faz o agente dormir um pouco para depois executar a instrução desejada
				     * */
				    //Vou percorrer a tag "chaves" no XML
				    //Vou pegando o valor de carga de cada chave
				    //Vou somando dentro do while
				    
				    //vou percorrer a tag "microrredes" pegando o valor de cada potencia disponivel das microrredes
				    //vou somando cada potência disponível de cada microrrede
				    
				    //Pensando bem, acho que dá pra fazer isso no método anterior. Se eu não fizer isso nas rotinas anteriores, vou fazer semelhante a elas de qualquer forma.
				    /*********************************************************************************
				     * Aqui é porque eu tenho certeza que há outros ALs, se não eu tenho que verificar a quantidade antes
					 * FIPA Contract Net Initiator para negociação com dispositivos de geração controlada
					*********************************************************************************/
				    double cargaTotalPerdida = cargaPerdida + cargaTotalDisponivelMicrorrede; 
				    
				    final ACLMessage negociarDeltaP = new ACLMessage(ACLMessage.CFP);
					List lista3 = agenteALBD.getChild("outrosALs").getChildren(); 
					Iterator i3 = lista3.iterator();
					
				    while(i3.hasNext()) {
				    	Element elemento = (Element) i3.next();
				    	String nome = String.valueOf(elemento.getName());
						
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes geradores no XML, então add ele como remetente
//														System.out.println("Entrou no if!!!!!");  //Só pra testar
				    		exibirAviso(myAgent, "Solicitando deltaP a " +nome+ "um valor de carga perdida igual à: "+cargaTotalPerdida);
				    		negociarDeltaP.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
				    	}
				    }	
				    
				    negociarDeltaP.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
				    negociarDeltaP.setContent(String.valueOf(cargaTotalPerdida)); 
				    
				    addBehaviour(new ContractNetInitiator(myAgent, negociarDeltaP) {
						
						protected void handlePropose(ACLMessage propose, Vector v) {
							exibirMensagem(propose);
							
//												ACLMessage reply = propose.createReply();
//												reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
//												v.addElement(reply);
						}
						
						protected void handleRefuse(ACLMessage refuse) {
							exibirAviso(myAgent, "Negociação recusada por " + refuse.getSender().getLocalName());
							
////											System.out.println(getLocalName() + ": Enviando resposta para " + negocia.getSender().getLocalName());
//												
//												ACLMessage aviso = negociarDeltaP.createReply();
//												aviso.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//												aviso.setPerformative(ACLMessage.REFUSE);
//												aviso.setContent("0");
//												comando
//												myAgent.send(aviso);
						}
						
						protected void handleFailure(ACLMessage failure) {
							if (failure.getSender().equals(myAgent.getAMS())) {
								// Notificação de erro feita pela plataforma
								exibirAviso(myAgent, "Não existe agentes armazenadores de energia");
							}
							else {
//								exibirAviso(myAgent, "-<<"+nomeAgente+">>: o agente "+failure.getSender().getLocalName()+" falhou");
							}										
						} //Fim do handleFailure
						
						protected void handleAllResponses(Vector responses, Vector acceptances) {

							// Evaluate proposals.
							double bestProposal = -1;
//												AID bestProposer = null;
							ACLMessage accept = null;
							
							Enumeration e = responses.elements();  //a variável "e" será uma espécie de vetor de elementos, onde os elementos serão as mensagens recebidas
							while (e.hasMoreElements()) { //um while só pra percorrer todas as posições do vetor "e"
								ACLMessage msgProposta = (ACLMessage) e.nextElement();  //a variável msg receberá a cada iteração uma mensagem recebida que corresponde a cada posição de "e"
								
								if (msgProposta.getPerformative() == ACLMessage.PROPOSE) { //Se a performativa da mensagem msg for uma proposta (PROPOSE), então entra no SE
									ACLMessage resposta = msgProposta.createReply(); //será criada então uma resposta para essa mensagem
									acceptances.addElement(resposta);
									
									double proposal = Double.parseDouble(msgProposta.getContent()); //Aqui ele pega a propostas para avaliá-la
									if (proposal > bestProposal) {
										bestProposal = proposal;
//															bestProposer = msgProposta.getSender();
//															
										resposta.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // seta a performativa logo como REject_PROPOSAL

//															accept = resposta;
//															accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
									}else{
										resposta.setPerformative(ACLMessage.REJECT_PROPOSAL); // seta a performativa logo como REject_PROPOSAL
										
									}
								}//Fim do if msg.getPErformative() == ACLMessage.PROPOSE
							}//Fim do while (e.hasMoreElements()) 
													
						}// Fim do handle all responses
						
						protected void handleInform(ACLMessage inform) {
//							System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
							
						}//Fim do handle Informr
					}); //Fim do contract net initiator

				} //Fim do if(cont>referenciaDaChaveAtuante)
			    else{ //se não tem chaves a jusante, mas ver se não tem pelo menos microrrede no mesmo trecho.Chegando aqui nada pode ser feito para recomposição. É um alimentador que possui 1 trecho somente
			    	exibirAviso(myAgent, "Não tem chaves a jusante. Vou ver se tem microrredes no trecho afetado que precisem ilhar.");
			    	//*********Saber se tem agente apc e quantos são
					List lista1 = agenteALBD.getChild("microrredes").getChild(agenteChaveSobFalta).getChildren(); 
					Iterator i1 = lista1.iterator();
					
					int contMicrorrede = 0; //inicia cont com zero
					
				    while(i1.hasNext()) { 
				    	Element elemento = (Element) i1.next();
				    	String nome = String.valueOf(elemento.getName());
				    	exibirAviso(myAgent, "Aqui é para aparecer o nome de um agente APC ou 'nenhum'.No caso está aparecendo: "+nome);
				    	
				    	if (nome!= null && nome.length()>0 && nome!="nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
				    		contMicrorrede = contMicrorrede + 1; //Se houver algum agente chave, incrementa o contador
				    	}
				    }
				    exibirAviso(myAgent,"O valor de contMicrorrede é: "+contMicrorrede);
				    
				    if(contMicrorrede>0){ // Se existirem microrredes
//				    if(cont>referenciaDaChaveAtuante){ //Se o número de agente chave for maior que o índice da chave atuante, é porque com certeza há chaves a jusante da chave atuante
						exibirAviso(myAgent, "Há microrredes no trecho afetado. Preciso avisá-las para ilharem.");
				    	/**********************************************************************************
					     * Protocolo FIPA Request para solicitar que APCs abram suas chaves
					     * 
					     *********************************************************************************/
				  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				  		msg.setContent("abra");
				  		
				  		
				  		List lista2 = agenteALBD.getChild("microrredes").getChild(agenteChaveSobFalta).getChildren();
						Iterator i2 = lista2.iterator();
						
					    while(i2.hasNext()) { 
					    	Element elemento = (Element) i2.next();
					    	String nome = String.valueOf(elemento.getName());
					    	
							exibirAviso(myAgent, "Analisando se aviso ao agente APC "+nome+" que comande a abertura de seu disjuntor.");
							
					    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ 
					    		
			    				exibirAviso(myAgent, "Solicitando ao agente chave "+nome+" que comande a abertura de sua chave.");
//					    				
					    		msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
							    		
					    	}// Fim do if para saber se há chave
					    }// Fim do while(i.hasNext())	
				  		
				  		
					    addBehaviour(new AchieveREInitiator(myAgent, msg) {
							protected void handleInform(ACLMessage inform) {
								System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
							}
							protected void handleRefuse(ACLMessage refuse) {
//								System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
//								nResponders--;
							}
							protected void handleFailure(ACLMessage failure) {
								if (failure.getSender().equals(myAgent.getAMS())) {
									// FAILURE notification from the JADE runtime: the receiver
									// does not exist
									System.out.println("Responder does not exist");
								}
								else {
									System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
								}
							}
							protected void handleAllResultNotifications(Vector notifications) {
//								if (notifications.size() < nResponders) {
//									// Some responder didn't reply within the specified timeout
//									System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
//								}
							}//fim do protect void handleAllResultNotifications
						}); //Fim do addBehaviour do Request para avisar as micrroredes do trecho onde houve falta
			    
				    }//If cont>0 (se tiver micrroredes no mesmo trecho onde houve falta
				    else{
				    	exibirAviso(myAgent,"Não há microrredes no trecho afetado.");
				    }
				}//Fim do if tiver chaves a jusante, senão verifica se não tem pelo menos microrrede no trecho afetado
				return resposta;
			}//fim de handleSubscription
			
		});	//Fim do SubscriptionResponder
		
		/********************************************************************************************************
		 * Protocolo FIPA Contract Net para receber solicitações de carga advindos de ALs circunvizinhos
		 ********************************************************************************************************/
		addBehaviour(new ContractNetResponder(this, filtroContractNet) {
			
			
			private static final long serialVersionUID = 1L;

			protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
				
								
				double cargaTotalDisponivel = 0;
				
				ArrayList minhasCargas = new ArrayList();
				minhasCargas.add("0"); //a posição zero da array terá o valor "0".
				
				//*********Verificar as cargas de cada trecho e add no vetor
				List lista = agenteALBD.getChild("chaves").getChildren(); 
				Iterator i = lista.iterator();
				
				int cont = 0; //Aproveito e já conto a quantidade de elementos que eu estou lidando
				
			    while(i.hasNext()) { 
			    	Element elemento = (Element) i.next();
			    	String nome = String.valueOf(elemento.getName());
			    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
			    		String cargaDoTrecho = elemento.getAttributeValue("carga");
			    		minhasCargas.add(cargaDoTrecho);
			    		
			    		cargaTotalDisponivel = cargaTotalDisponivel + Double.parseDouble(cargaDoTrecho);
			    		cont = cont+1;
			    	}
			    }
				
			    double cargaSolicitada = Double.parseDouble(cfp.getContent()); //Carga solicitada por outro AL	
			    double cargaTotalNoTrecho = 0;
			    double cargaTrecho = 0;
			    
			  //*********Verificar se há sobrecarga
				for (int j = 0; j < cont+1; j++){
//					double capa
					
					
				}
			    
			    
//				return cfp;
////				System.out.println("Agent "+getLocalName()+": CFP received from "+cfp.getSender().getName()+". Action is "+cfp.getContent());
//				exibirMensagem(cfp);
//							
////				valorSOC = Double.parseDouble(((Element) agenteAABD.getContent()).getText());
//				double valorSOC = Double.parseDouble(agenteALBD.getChild("medidasAtuais").getChild("soc").getText());
//				exibirAviso(myAgent, "O SOC está em "+valorSOC);
//				
//				if (valorSOC > 80) {
//					// We provide a proposal
////					System.out.println("Agent "+getLocalName()+": Proposing "+proposal);
//					exibirAviso(myAgent, "Aceito a solicitação de deltaP igual a: "+cfp.getContent());
					ACLMessage propose = cfp.createReply();
					propose.setPerformative(ACLMessage.PROPOSE);
//					propose.setContent(String.valueOf(valorSOC));
					propose.setContent(cfp.getContent());
					return propose;
//				}
//				else {
//					// We refuse to provide a proposal
////					System.out.println("Agent "+getLocalName()+": Refuse");
//					exibirAviso(myAgent, "Recusei o pedido de deltaP");
//					throw new RefuseException("O SOC da bateria está abaixo de SOC!");
//				}
			}

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
////				System.out.println("Agent "+getLocalName()+": Proposal accepted");
////				if (performAction()) {
////					System.out.println("Agent "+getLocalName()+": Action successfully performed");
					ACLMessage inform = accept.createReply();
					inform.setPerformative(ACLMessage.INFORM);
//					
//					//Antes eu dou uma atualizada no XML
//					agenteAABD.getChild("comando").getChild("estadoChave").setText("1"); //seta o XML o disjuntor fechando
//					agenteAABD.getChild("comando").getChild("status").setText("0"); //seta no XML o modo de tensão pois no matlab tem um NOT que enviará 1 para a fonte de tensão
//					
					return inform;
////				}
////				else {
////					System.out.println("Agent "+getLocalName()+": Action execution failed");
////					throw new FailureException("unexpected-error");
////				}	
			}

			protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				System.out.println("Agent "+getLocalName()+": Proposal rejected");
				
				
			}
		} ); //Fim do comportamento contract net
	} // fim do public void setup
		

	public void exibirMensagem(ACLMessage msg){
		
		System.out.println("\n\n===============<<MENSAGEM>>==============");
		System.out.println("De: " + msg.getSender());
		System.out.println("Para: " + this.getName());
		System.out.println("Conteudo: " + msg.getContent());
		System.out.println("=============================================");
	}
	
public void exibirAviso(Agent myAgent, String aviso){
    	
		System.out.println("\n\n-----------------<<AVISO>>------------------");
		System.out.println("Agente: "+myAgent.getLocalName());
		System.out.println("Aviso: " +aviso);
		
		Calendar cal = Calendar.getInstance();
    	cal.getTime();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//    	SimpleDateFormat sdf = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ssss a zzz");
    	SimpleDateFormat sdf = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ssss a");
    	System.out.println( sdf.format(cal.getTime()) );
    	
//		System.out.println(System . currentTimeMillis ());
	}

	/**
	 *  Método para carregamento do XML
	 * 
	 */
	public Element carregaBD(String nomeAgente) {
	
		// Declaração de variáveis, uma de cada tipo
		File endereco = null; // Desnecessário se usarmos o endereço do arq.
								// dentro do doc dentro do try
		SAXBuilder builder = null; // usado p/ processar a estrut. do doc. p/
									// dentro da variável do tipo documento
		Document doc = null;
		Element BD = null;
	
		endereco = new File("src/XML/" + nomeAgente + ".xml");
	
		builder = new SAXBuilder();
	
		try { // Criando o arquivo propriamente dito
			doc = builder.build(endereco);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}// fim do try
	
		BD = doc.getRootElement();
		return BD;
	
	}// fim do método carregarBD
	
}
