/************************************************************************************************************
 * @author Fernando Américo Albuquerque Rodrigues Marçal
 * @since 09/04/2015
 * @version 1.0
 * Descrição
 * Este é o comportamento do Agente do ponto de conexão (APC) 
 * 
 *               ▬▬▬▬▬▬▬▬▬▬▬ஜ۩۞۩ஜ▬▬▬▬▬▬▬▬▬▬▬▬▬
 * << ▓▓▓▒▒▒░░░  Lista de Abreviaturas ░░░▒▒▒▓▓▓ >>
 *               
 * nomeAgente: variável que receberá o nome do agente em questão
 * Mensagem: variável que receberá a mensagem ACLMessage
 * BD: banco de dados
 * CR : conexão a rede. Diz se a urrede está conectada a rede e consequente o estado da chave do PCC. 1-conectado/fechado 0-
 ************************************************************************************************************/


import jade.core.Agent;
//import java.util.Iterator;
import jade.lang.acl.ACLMessage; //Relacionada a endereçoes
import jade.core.AID;    //Relacionada a endereços
import jade.lang.acl.MessageTemplate; // Para uso dos filtros

//Protocolos de comunicação
import jade.proto.AchieveREInitiator;
import jade.proto.SubscriptionInitiator;
import jade.domain.FIPANames; //Foi solicitado no protocolo suscribe 
import jade.proto.ContractNetInitiator;
import java.util.Vector;

//Comportamentos cíclicos
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;


//Bibliotecas para lidar com arquivos XML
//import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder; //This package support classes for building JDOM documents and content using SAX parsers. 
//import org.jdom2.Attribute;


import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;







//Foram incluídas automaticamente
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

//Importados automaticamente. Para tratar de listas
import java.util.List;
import java.util.List; //Trantando com lista

public class agentePC extends Agent { /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

// Classe "agentePC" que por sua vez é uma subclasse
									// da classe "Agent"
	double PotenciaGeracaoTotal, valorPotGerRecebido = 0; //Inicialização da potência gerada por gerações intermitentes (
	double PotenciaDemandaTotal, valorPotDemRecebido = 0; //Inicialização da potência demandada pelas cargas
	double deltaP = 0;  //Inicialização do balanço de potência
	
	public void setup()
	{
				
		final String nomeAgente = getLocalName(); //a variável "nomeAgente" recebe o nome local do agente 
		final Element agenteApcBD = carregaBD(nomeAgente); //Chama o método carregaBD que carrega o BD do agente "nomeAgente"
		
		//Filtro para receber somente mensagens do protocolo tipo "inform"
//		MessageTemplate filtroInformMonitoramento = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		final MessageTemplate filtroInformMonitoramento = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchSender(getAID("tcpACP1")));
		
		
//		System.out.println(".:: Agente PCC APCC1 iniciado com sucesso! ::.\n");
//		System.out.println("Todas as minhas informações: \n" +getAID());
//		System.out.println(">> Meu nome local é " + getLocalName()); // Informações completas
//		System.out.println("\n>> Meu nome global é " +getAID().getName());
//		System.out.println("Meu endereço é " +getAID().getAllAddresses());
		
//		System.out.println("\nMeus endereços são: ");
//		Iterator it = getAID().getAllAddresses();
//		while(it.hasNext()){
//			System.out.println("- "+it.next());
//		}

		
		addBehaviour(new TickerBehaviour(this,100) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void onTick(){
				deltaP = 0;
				ACLMessage msg = receive(filtroInformMonitoramento);
				//String conteudo = mensagem.getContent();

				//if(msg_curto!=null && msg_curto.getContent()=="curto"){
				//if(msg_curto!=null && conteudo=="curto"){
				if(msg!=null){	
					exibirMensagem(msg);
					String cr = msg.getContent();
					
					if(cr.equals("0")) {

					exibirAviso(myAgent,"O PCC está aberto!"); //Só pra ver se deu certo
					
					agenteApcBD.getChild("cr").setText(msg.getContent()); //Seta no XML o CR
					//Ao receber a mensagem Inform do estado do PCC, não é necessário responder a mensagem
//						ACLMessage resposta = filtro_Inform.createReply();
//						resposta.setPerformative(ACLMessage.AGREE);
//						resposta.setContent("Recebido!");
//						myAgent.send(resposta);
					
//						String cr =  agenteApcBD.getChild("cr").getText(); //Dá no mesmo que a seguinte linha de código
//						String cr =  agenteApcBD.getChildText("cr");
					
					/*
					 * Eu pus um delay porque primeiro o APC, AG e AA devem receber os dados do Matlab para somente depois 
					 * o APC começar a analisar o balanço de energia e começar uma coordenação de tudo.
					 */
//					try {
//					    Thread.sleep(10000);                 //Delay em milisegundos
//					} catch(InterruptedException ex) {
//					    Thread.currentThread().interrupt();
//					}
					
					
					/**
				     * Protocolo FIPA Request para solicitar que o dispositivo de armazenamento atue fechando sua chave e servindo de fonte de tensão
				     * 
				     */
			  		ACLMessage msg1 = new ACLMessage(ACLMessage.REQUEST);
			  		msg1.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				  		
			  		//Cria-se uma lista para percorrer a tag agentesArmazenamento e ver seus elementos
			  		List lista3 = agenteApcBD.getChild("agentesArmazenamento").getChildren(); 
					Iterator i3 = lista3.iterator();
					
				    while(i3.hasNext()) { 
				    	Element elemento = (Element) i3.next();
				    	String nome = String.valueOf(elemento.getName());
						
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes de armazenamento no XML, então add ele como remetente
//									System.out.println("Entrou no if!!!!!");  //Só pra testar
				    		exibirAviso(myAgent, "Solicitando a "+nome+"que atue como fonte de tensão.");
				    		msg1.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
				    	}
				    }		
				  
				    msg1.setContent("ilhou");
			  		
			  		addBehaviour(new AchieveREInitiator(myAgent, msg1) {
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
					}); //Fim do addBehaviour do request initiator
					
					
					/********************************************************************************
					 * FIPA Subscribe Initiator para saber o valor de Potência dos dispositivos de geração intermitente
					 ********************************************************************************/	
					ACLMessage msgColetarPot = new ACLMessage(ACLMessage.SUBSCRIBE); // Campo da mensagem SUBSCRIBE
					msgColetarPot.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
					msgColetarPot.setContent("Potencia");
					
					/**
					 * Aqui vamos acessar o XML do APC para pesquisar o nome dos agentes de geração intermitente
					 * e cargas para calcular o balanço de potência na microrrede
					 * */
					List lista = agenteApcBD.getChild("agentesGeracaoNaoControladas").getChildren(); 
//							System.out.println("O nome dos AGs intermitentes são: "+lista);
					Iterator i = lista.iterator();
					
				    while(i.hasNext()) {
				    	Element elemento = (Element) i.next();
				    	String nome = String.valueOf(elemento.getName());
//						    	System.out.println("O nome é: "+nome);  //Só pra testar 
				    	
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes geradores no XML, então add ele como remetente
//									System.out.println("Entrou no if!!!!!");  //Só pra testar
				    		exibirAviso(myAgent, "Solicitando valor de potencia gerada a " +nome);
				    		msgColetarPot.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
				    	}
				    }
				    
				    addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
				    	
						private static final long serialVersionUID = 1L; //Posto automaticamente
//						double PotenciaGeracaoTotal, valorPotGerRecebido = 0; //Inicialização da potência gerada por gerações intermitentes (
//						double PotenciaDemandaTotal, valorPotDemRecebido = 0; //Inicialização da potência demandada pelas cargas
//					    double deltaP = 0;  //Inicialização do balanço de potência
					    
						/* No handleAgree eu vou coletando os valores de todas as gerações e vou armazenando na variável PotenciaGeracaoI
						 * (non-Javadoc)
						 * @see jade.proto.SubscriptionInitiator#handleAgree(jade.lang.acl.ACLMessage)
						 */
						protected void handleAgree(ACLMessage agree){
							exibirMensagem(agree);
							double valorRecebido = Double.parseDouble(agree.getContent());
							valorPotGerRecebido = Double.parseDouble(agree.getContent());
							exibirAviso(myAgent, "Recebi um valor de "+valorPotGerRecebido+" de "+agree.getSender());
							PotenciaGeracaoTotal = PotenciaGeracaoTotal + valorPotGerRecebido; 
							valorPotGerRecebido = 0;
							
//							exibirAviso(myAgent, "O valor da potencia gerada é: "+PotenciaGeracaoI);
				    	}//Fim do handleAgree do Subscribe
				
						protected void handleRefuse(ACLMessage refuse) { //Se recusar
							
						}// Fim do handleRefuse do Subscribe
						protected void handleFailure(ACLMessage failure) { //Se erro
							
						}// Fim do handleFailure do Subscribe
				    }); // Fim do comportamento FIPA Subscribe -> addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
				    
				    
				    /********************************************************************************
					 * FIPA Subscribe Initiator para saber o valor de Potência Demandada pelas cargas
					 ********************************************************************************/	
					ACLMessage msgColetarPotCargas = new ACLMessage(ACLMessage.SUBSCRIBE); // Campo da mensagem SUBSCRIBE
					msgColetarPotCargas.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
					msgColetarPotCargas.setContent("Potencia");
					
					/**
					 * Aqui vamos acessar o XML do APC para pesquisar o nome dos agentes de geração intermitente
					 * e cargas para calcular o balanço de potência na microrrede
					 * */
					List lista2 = agenteApcBD.getChild("agentesCarga").getChildren(); 
//					System.out.println("O nome dos ADs são: "+lista2); //Só pra ver se deu certo
					Iterator i2 = lista2.iterator();
					
				    while(i2.hasNext()) {
				    	Element elemento = (Element) i2.next();
				    	String nome = String.valueOf(elemento.getName());
//						System.out.println("O nome é: "+nome);  //Só pra testar 
//				    	exibirAviso(myAgent, "Solicitando valor de potencia demandada a: "+nome); //Só pra ver se deu certo
						
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes geradores no XML, então add ele como remetente
//									System.out.println("Entrou no if!!!!!");  //Só pra testar
				    		exibirAviso(myAgent, "Solicitando valor de carga a " +nome);
				    		msgColetarPotCargas.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
				    	}
				    }
				    
				    addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPotCargas){
				   
						private static final long serialVersionUID = 1L; //Posto automaticamente
//						double PotenciaGeracaoTotal, valorPotGerRecebido = 0; //Inicialização da potência gerada por gerações intermitentes (
//						double PotenciaDemandaTotal, valorPotDemRecebido = 0; //Inicialização da potência demandada pelas cargas
					    
						/* No handleAgree eu vou coletando os valores de todas as gerações e vou armazenando na variável PotenciaGeracaoI
						 * (non-Javadoc)
						 * @see jade.proto.SubscriptionInitiator#handleAgree(jade.lang.acl.ACLMessage)
						 */
						protected void handleAgree(ACLMessage agree){
							exibirMensagem(agree);
//							double valorRecebido = Double.parseDouble(agree.getContent());
							valorPotDemRecebido = Double.parseDouble(agree.getContent());
							exibirAviso(myAgent, "Recebi um valor de "+valorPotDemRecebido+" de "+agree.getSender());
							PotenciaDemandaTotal = PotenciaDemandaTotal + valorPotDemRecebido; 
							valorPotDemRecebido = 0;
//							exibirAviso(myAgent, "O valor da potencia gerada é: "+PotenciaGeracaoI);
							
							
				    	}//Fim do handleAgree do Subscribe
				
						protected void handleRefuse(ACLMessage refuse) { //Se recusar
							
						}// Fim do handleRefuse do Subscribe
						protected void handleFailure(ACLMessage failure) { //Se erro
							
						}// Fim do handleFailure do Subscribe
				    }); // Fim do comportamento FIPA Subscribe -> addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPotCarga){
				    
//				    try {
//					    Thread.sleep(10000);                 //Delay em milisegundos
//					} catch(InterruptedException ex) {
//					    Thread.currentThread().interrupt();
//					}
				    
//				     Add the WakerBehaviour (wakeup-time 1 sec)
				    addBehaviour(new WakerBehaviour(myAgent, 1000) {
				    	protected void handleElapsedTimeout() {
				    		exibirAviso(myAgent, "Agent "+myAgent.getLocalName()+": It's wakeup-time. Bye...");
						    exibirAviso(myAgent, "A potência gerada total é: "+PotenciaGeracaoTotal);
						    exibirAviso(myAgent, "A potência demandada total é: "+PotenciaDemandaTotal);
						    deltaP = PotenciaGeracaoTotal - PotenciaDemandaTotal;
						    exibirAviso(myAgent, "O balanço de potência atual é: "+deltaP);
						   
							PotenciaGeracaoTotal = 0;
							PotenciaDemandaTotal = 0; //No final de tudo zera essas variáveis para começar todo o processo novamente
							
						    						    
						    //Obs.: Por enquanto vou colocar valores aleatórios para cacular o balanço de potência na microrrede
						    // Mas ai tenho que já ter uma base da potência da microrrede, das cargas...
							
							/**
							 * Aqui supõe-se que eu já enviei um subscribe para o AG intermitente e cargas
							 * e tenho o valor do balanço de potência na microrrede (deltaP)
							 */
//							double deltaP = -1000; //Balanço de potência na microrrede. 
//							System.out.println("O valor do balanço é "+deltaP); //Só pra testar

							/**
							 * Um valor positivo de deltaP diz que tá sobrando energia. Um valor negativo, diz que tá com déficit, ou seja, 
							 * não tem geração suficiente.
							 * Se deltaP positivo tá tudo ok. 
							 * Se deltaP negativo, então verifica-se se tem dispositivos armazenadores de energia. 
							 *  */
							if(deltaP<0){ 
								/**se entrar nesse if, será necessário consultar o XMl para averiguar se há dispositivos
								 * armazenadores de energia
								 */
								exibirAviso(myAgent, "Entrei dentro do if do deltaP");
								/*********************************************************************************
								 * FIPA Contract Net Initiator para negociação com dispositivos de armazenamento
								*********************************************************************************/
								final ACLMessage negociarDeltaP = new ACLMessage(ACLMessage.CFP);
								List lista3 = agenteApcBD.getChild("agentesArmazenamento").getChildren(); 
								Iterator i3 = lista3.iterator();
								
							    while(i3.hasNext()) 				{
							    	Element elemento = (Element) i3.next();
							    	String nome = String.valueOf(elemento.getName());
									
							    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes geradores no XML, então add ele como remetente
//												System.out.println("Entrou no if!!!!!");  //Só pra testar
							    		exibirAviso(myAgent, "Solicitando deltaP a " +nome);
							    		negociarDeltaP.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
							    	}
							    	double PotenciaGeracaoTotal, valorPotGerRecebido = 0; //Inicialização da potência gerada por gerações intermitentes (
							    	double PotenciaDemandaTotal, valorPotDemRecebido = 0; //Inicialização da potência demandada pelas cargas
//							    	double deltaP = 0;    
							    }		
							    negociarDeltaP.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
							    deltaP = Math.abs(deltaP); //módulo de deltaP
							    negociarDeltaP.setContent(String.valueOf(deltaP)); 
							    
							    addBehaviour(new ContractNetInitiator(myAgent, negociarDeltaP) {
									
									protected void handlePropose(ACLMessage propose, Vector v) {
										exibirMensagem(propose);
										
//										ACLMessage reply = propose.createReply();
//										reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
//										v.addElement(reply);
									}
									
									protected void handleRefuse(ACLMessage refuse) {
										exibirAviso(myAgent, "Negociação recusada por " + refuse.getSender().getLocalName());

////									System.out.println(getLocalName() + ": Enviando resposta para " + negocia.getSender().getLocalName());
//										
//										ACLMessage aviso = negociarDeltaP.createReply();
//										aviso.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//										aviso.setPerformative(ACLMessage.REFUSE);
//										aviso.setContent("0");
//										
//										myAgent.send(aviso);
										
										/*********************************************************************************
										 * FIPA Contract Net Initiator para negociação com dispositivos de geração controlada
										*********************************************************************************/
										final ACLMessage negociarDeltaP = new ACLMessage(ACLMessage.CFP);
										List lista3 = agenteApcBD.getChild("agentesGeracaoControladas").getChildren(); 
										Iterator i3 = lista3.iterator();
										
									    while(i3.hasNext()) 				{
									    	Element elemento = (Element) i3.next();
									    	String nome = String.valueOf(elemento.getName());
											
									    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes geradores no XML, então add ele como remetente
//														System.out.println("Entrou no if!!!!!");  //Só pra testar
									    		exibirAviso(myAgent, "Solicitando deltaP a " +nome);
									    		negociarDeltaP.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
									    	}
									    }		
									    negociarDeltaP.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
									    exibirAviso(myAgent, "o valor de deltaP é: "+deltaP);
									    deltaP = Math.abs(deltaP); //módulo de deltaP
									    negociarDeltaP.setContent(String.valueOf(deltaP)); 
									    
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
//												
//												myAgent.send(aviso);
											}
											
											protected void handleFailure(ACLMessage failure) {
												if (failure.getSender().equals(myAgent.getAMS())) {
													// Notificação de erro feita pela plataforma
													exibirAviso(myAgent, "Não existe agentes armazenadores de energia");
												}
												else {
													exibirAviso(myAgent, "-<<"+nomeAgente+">>: o agente "+failure.getSender().getLocalName()+" falhou");
												}										
											} //Fim do handleFailure
											
											protected void handleAllResponses(Vector responses, Vector acceptances) {
//												ACLMessage reply = responses.createReply();
//												reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
//												reply.setContent("");
												
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
												}
												
												
												// Accept the proposal of the best proposer
//												if (accept != null) {
//													exibirAviso(myAgent,"Accepting proposal "+bestProposal+" from responder "+bestProposer.getName());
//													accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
//												}	
																		
											}// Fim do handle all responses
											
											protected void handleInform(ACLMessage inform) {
												System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
											}
										}); //Fim do contract net initiator
									}
									
									protected void handleFailure(ACLMessage failure) {
										if (failure.getSender().equals(myAgent.getAMS())) {
											// Notificação de erro feita pela plataforma
											exibirAviso(myAgent, "Não existe agentes armazenadores de energia");
										}
										else {
											exibirAviso(myAgent, "-<<"+nomeAgente+">>: o agente "+failure.getSender().getLocalName()+" falhou");
										}										
									} //Fim do handleFailure
									
									protected void handleAllResponses(Vector responses, Vector acceptances) {
//										ACLMessage reply = responses.createReply();
//										reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
//										reply.setContent("");
										
										// Evaluate proposals.
										double bestProposal = -1;
//										AID bestProposer = null;
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
//													bestProposer = msgProposta.getSender();
//													
													resposta.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // seta a performativa logo como REject_PROPOSAL

//													accept = resposta;
//													accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
												}else{
													resposta.setPerformative(ACLMessage.REJECT_PROPOSAL); // seta a performativa logo como REject_PROPOSAL
													
													
													
												}
											}//Fim do if msg.getPErformative() == ACLMessage.PROPOSE
										}
										
										
										// Accept the proposal of the best proposer
//										if (accept != null) {
//											exibirAviso(myAgent,"Accepting proposal "+bestProposal+" from responder "+bestProposer.getName());
//											accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
//										}	
																
									}// Fim do handle all responses
									
									protected void handleInform(ACLMessage inform) {
										System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
									}
								}); //Fim do contract net initiator
							  
								
//							    deltaP = 0;
								PotenciaGeracaoTotal = 0;
								PotenciaDemandaTotal = 0; //No final de tudo zera essas variáveis para começar todo o processo novamente
								
							    
//								}//Fim do if deltaP<0
								}else{
									exibirAviso(myAgent, "deltaP é maior que zero (deltaP>0).");
								}
				    		}
				    	}); //Fim do WakerBehaviour
					
					    
			  		
					}else if(msg.getContent().equals("1")){
						exibirAviso(myAgent, "O PCC está fechado!");
						
					}else{
						exibirAviso(myAgent, "O PCC não está acusando nem 0 nem 1!! Problema");
					}
				
//					deltaP = PotenciaGeracaoTotal = PotenciaDemandaTotal= 0; //No final de tudo zera essas variáveis para começar todo o processo novamente

//					deltaP = 0;
//					PotenciaGeracaoTotal = 0;
//					PotenciaDemandaTotal = 0; //No final de tudo zera essas variáveis para começar todo o processo novamente
					
			}// fim o if para saber se inform != null
			} // fim do onTick 
		}); //fim do comportamento temporal TickerBehaviour
		
		

					
					
				

	} // fim do public void setup

	/**
	 * Método para exibição de mensagens ACL quando há comunicação entre agentes ou matlab e agentes
	 *  @param msg recebe uma mensagem to tipo ALCMessage
	 *  
	 */
	public void exibirMensagem(ACLMessage msg) {
		System.out.println("\n\n===============<<MENSAGEM>>==================");    	
		System.out.println("De: " + msg.getSender());
		System.out.println("Para: " + this.getName());
		System.out.println("Performativa: "+msg.getPerformative());
		System.out.println("Protocolo: "+msg.getProtocol());
		System.out.println("Conteudo: " + msg.getContent());
		
		Calendar cal = Calendar.getInstance();
    	cal.getTime();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//    	SimpleDateFormat sdf = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ssss a zzz");
    	SimpleDateFormat sdf = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ssssss a");
    	System.out.println( sdf.format(cal.getTime()) );
    	
//		System.out.println(System . currentTimeMillis ());
		
	}
	
	/**
	 * Método para exibição de aviso do próprio agente
	 *  @param msg recebe uma mensagem to tipo ALCMessage
	 *  
	 */
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
