import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BlackJack {
	private class Card {
		String value;
		String type;

		Card(String value, String type) {
			this.value = value;
			this.type = type;
		}

		public String toString() {
			return value + " of " + type;
		}

		public int getValue() {
			if ("AceJackQueenKing".contains(value)) { // Ace Jack Queen King
				if (value == "Ace") {
					return 11;
				}
				return 10;
			}
			return Integer.parseInt(value); // 2-10
		}

		public boolean isAce() {
			return value == "Ace";
		}

		public String getImagePath() {
			return "cards/" + toString() + ".png";
		}
	}

	ArrayList<Card> deck;
	Random random = new Random(); // shuffle deck

	// dealer
	Card hiddenCard; // the dealer's hidden card
	ArrayList<Card> dealerHand; // what cards the dealer has
	int dealerHandSum; // the sum of the cards in the dealers hand
	int dealerAceCount; // the count of aces in the dealers hand

	// player
	ArrayList<Card> playerHand; // what cards the player has
	int playerHandSum; // the sum of the cards in the players hand
	int playerAceCount; // the count of aces in the players hand
	int playerChips; // the total count of the player's chips
	int playerBet; // the amount of chips the user has bet on the hand

	// window
	int boardWidth = 1000; // set the width of the board
	int boardHeight = 600; // set the height of the board

	int cardWidth = 110; // set the width of the cards
	int cardHeight = 154; // set the height of the cards

	JFrame frame = new JFrame("Black Jack");
	JPanel gamePanel = new JPanel() {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			try {
				// draw the hidden card
				Image hiddenCardImage = new ImageIcon(getClass().getResource("cards/BACK.png")).getImage();
				if (!standButton.isEnabled()) {
					hiddenCardImage = new ImageIcon(getClass().getResource(hiddenCard.getImagePath())).getImage();
				}
				g.drawImage(hiddenCardImage, 20, 20, cardWidth, cardHeight, null);

				// draw the dealer's cards
				for (int i = 0; i < dealerHand.size(); i++) {
					Card card = dealerHand.get(i);
					Image cardImage = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
					g.drawImage(cardImage, cardWidth + 25 + (cardWidth + 5) * i, 20, cardWidth, cardHeight, null);
				}

				// draw the player's cards
				for (int i = 0; i < playerHand.size(); i++) {
					Card card = playerHand.get(i);
					Image cardImage = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
					g.drawImage(cardImage, 20 + (cardWidth + 5) * i, 320, cardWidth, cardHeight, null);
				}

				//handle game ending and player's bets
				if (!standButton.isEnabled()) {
					dealerHandSum = reduceDealerAce();
					playerHandSum = reducePlayerAce();

					String message = "";
					
					//both player and user have blackjack
					if(playerHandSum == 21 && dealerHandSum == 21){
						message = "Pushed! " + playerBet + " has been returned";
					} //player has blackjack and the dealer does not 
					else if(playerHandSum == 21 && dealerHandSum != 21) {
						message = "You have Blackjack! You won " + ((playerBet*3)/2) + " chips";
						raisePlayerChips(playerBet, 1.5);
					} //dealer has blackjack and the player does not 
					else if(dealerHandSum == 21 && playerHandSum != 21) {
						message = ("Dealer has Blackjack! You lost " + playerBet + " chips");
						lowerPlayerChips(playerBet);
					} //player busts 
					else if(playerHandSum > 21) {
						message = "You busted! You lost " + playerBet + " chips";
						lowerPlayerChips(playerBet);
					} //dealer busts 
					else if(dealerHandSum > 21) {
						message = "Dealer busted! You won " + playerBet + " chips";
						raisePlayerChips(playerBet, 1);
					}  //player has a greater hand than the dealer
					else if(playerHandSum > dealerHandSum) {
						message = "You won " +  playerBet + " chips";
						raisePlayerChips(playerBet, 1);
					} //dealer has a greater hand than the dealer
					else if(dealerHandSum > playerHandSum) {
						message = "You lost " + playerBet + " chips";
						lowerPlayerChips(playerBet);
					}

					g.setFont(new Font("Arial", Font.PLAIN, 30));
					g.setColor(Color.white);
					g.drawString(message, 220, 250);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	JPanel buttonPanel = new JPanel(); // create a panel for the buttons
	JButton hitButton = new JButton("Hit"); // create a button for the player to hit
	JButton standButton = new JButton("Stand"); // create a button for the player to stay
	JLabel chipsIndicator = new JLabel("Current total: " + playerChips + " Current Bet: " + playerBet);
	JButton playAgainButton = new JButton("Play Again"); //create a button so you can play again

	BlackJack() {
		startGame();

		// set the attributes of the frame
		frame.setVisible(true);
		frame.setSize(boardWidth, boardHeight);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// set the attributes of the game panel
		gamePanel.setLayout(new BorderLayout());
		gamePanel.setBackground(new Color(53, 101, 77));
		frame.add(gamePanel);
		System.out.println("In graphics");
		// set attributes for the hit, stay, and setBet button and the bet text field
		hitButton.setFocusable(false);
		buttonPanel.add(hitButton);
		standButton.setFocusable(false);
		buttonPanel.add(standButton);
		buttonPanel.add(chipsIndicator);
		frame.add(buttonPanel, BorderLayout.SOUTH);

		// handle when the player presses the hit button
		hitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Card card = deck.remove(deck.size() - 1); // get the card the player will get from hitting
				playerHandSum += card.getValue(); // get the value of the card
				playerAceCount += card.isAce() ? 1 : 0; // check if the card is an ace if so increase playerAceCount by 1
				playerHand.add(card); // add the card to the player's hand
				if (reducePlayerAce() > 21) { // if the player is over 21 but has an ace reduce the value from 11 to 1
					hitButton.setEnabled(false);
				}
				gamePanel.repaint();
			}
		});

		// handle when the player presses the stay button
		standButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hitButton.setEnabled(false);
				standButton.setEnabled(false);

				while (dealerHandSum < 17) { // the dealer will only get another card if their card total is below 17
					Card card = deck.remove(deck.size() - 1); // get the card the dealer will get
					dealerHandSum += card.getValue(); // get the value of the card
					dealerAceCount += card.isAce() ? 1 : 0; // check if the card is an ace if so increase dealerAceCount by 1
					dealerHand.add(card); // add the card to the dealer's hand
				}
				gamePanel.repaint();
			}
		});

		gamePanel.repaint();
	}

	public void startGame() {
		// deck
		buildDeck(); //build the deck of cards
		shuffleDeck(); //shuffle the deck of cards
		setPlayerChips(); //set the players total chips before the hand
		setUserBet(); //set the user's bet for the hand

		// dealer
		dealerHand = new ArrayList<Card>();
		dealerHandSum = 0; // initialize the sum of the dealer's cards
		dealerAceCount = 0; // initialize the count of aces in the dealer's hand

		// get the dealer's hidden card
		hiddenCard = deck.remove(deck.size() - 1); // remove card at last index
		dealerHandSum += hiddenCard.getValue();
		dealerAceCount += hiddenCard.isAce() ? 1 : 0;

		// get the dealer's one face up card
		Card card = deck.remove(deck.size() - 1);
		dealerHandSum += card.getValue();
		dealerAceCount += card.isAce() ? 1 : 0;
		dealerHand.add(card);

		// player
		playerHand = new ArrayList<Card>();
		playerHandSum = 0; // initialize the sum of the player's cards
		playerAceCount = 0; // initialize the count of aces in the player's hand

		// give the player their two starting cards
		for (int i = 0; i < 2; i++) {
			card = deck.remove(deck.size() - 1);
			playerHandSum += card.getValue();
			playerAceCount += card.isAce() ? 1 : 0;
			playerHand.add(card);
		}
	}


	public void buildDeck() {
		deck = new ArrayList<Card>();
		String[] values = { "Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King" };
		String[] types = { "Clubs", "Diamonds", "Hearts", "Spades" };

		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < values.length; j++) {
				Card card = new Card(values[j], types[i]);
				deck.add(card);
			}
		}
	}

	// method to shuffle the deck
	public void shuffleDeck() {
		for (int i = 0; i < deck.size(); i++) {
			int j = random.nextInt(deck.size());
			Card currCard = deck.get(i);
			Card randomCard = deck.get(j);
			deck.set(i, randomCard);
			deck.set(j, currCard);
		}
	}
	
	public void setPlayerChips() {
		playerChips = 50000;
		System.out.println("Player chips: " + playerChips);
		
	}

	//get the user's bet
	public void setUserBet() {
	    while (true) {
	        String betFieldText = JOptionPane.showInputDialog(null, "Enter your bet (Remaing chips: " + playerChips + "):");

	        try {
	            if (betFieldText == null || betFieldText.trim().isEmpty()) {
	                throw new NumberFormatException();
	            }

	            // Try to parse the input as an integer
	            playerBet = Integer.parseInt(betFieldText);

	            // Check if the parsed number is within the valid range
	            if (playerBet < 10 || playerBet > 50000 || playerBet > playerChips) {
	                throw new NumberFormatException();
	            }
	            // If everything is valid, break out of the loop
	            break;
	        } catch (NumberFormatException nfe) {
	            // Handle if the input is empty, not a number, or outside the valid range
	            JOptionPane.showMessageDialog(null, "Invalid Bet: Please enter a number between 10 and 50000 and it must be under " + playerChips,
	                    "Invalid Bet", JOptionPane.ERROR_MESSAGE);
	        }
	    }
	    System.out.println("Player1 bet " + playerBet + " chips");
	}
	
	public void lowerPlayerChips(int playerBet) {
		System.out.println("Player chips before lowering " + playerChips);
		playerChips -= playerBet;
		System.out.println("Player chips after lowering " + playerChips);
	}
	
	public void raisePlayerChips(int playerBet, double betModifier) {
		System.out.println("Player chips before raising " + playerChips);
		playerChips += (playerBet * betModifier);
		System.out.println("Player chips after raising " + playerChips);
	}


	// method to reduce the player's score if they go over 21 and have an ace in their hand
	public int reducePlayerAce() {
		while (playerHandSum > 21 && playerAceCount > 0) {
			playerHandSum -= 10;
			playerAceCount -= 1;
		}
		return playerHandSum;
	}

	// method to reduce the dealer's score if they go over 21 and have an ace in their hand
	public int reduceDealerAce() {
		while (dealerHandSum > 21 && dealerAceCount > 0) {
			dealerHandSum -= 10;
			dealerAceCount -= 1;
		}
		return dealerHandSum;
	}
}
